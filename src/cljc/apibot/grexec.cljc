(ns apibot.grexec
  "GrExec: Graph Execution"
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])
    #?(:cljs [apibot.state :refer [*app-state]])
    #?(:cljs [apibot.api :as api])
    [apibot.coll :as coll]
    [apibot.el :as el]
    [apibot.exec-history :as exec-history]
    [apibot.graphs :as graphs]
    [apibot.grexec.assert-body-node :as assert-body-node]
    [apibot.grexec.assert-headers-node :as assert-headers-node]
    [apibot.grexec.assert-node :as assert-node]
    [apibot.grexec.assert-status-node :as assert-status-node]
    [apibot.grexec.config-node :as config-node]
    [apibot.grexec.csv-node :as csv-node]
    [apibot.grexec.evaljs-node :as evaljs-node]
    [apibot.grexec.extract-body-node :as extract-body-node]
    [apibot.grexec.extract-header-node :as extract-header-node]
    [apibot.grexec.http-node :as http-node]
    [cats.monad.either :refer [branch]]
    [promesa.core :as p]))

;; ---- Specs ----

(s/def ::name (s/and (complement empty?) string?))
(s/def ::description string?)

;; ---- Funcs ----

(def graphs
  [;; a sorted vector of builtin graphs
   assert-body-node/graph
   assert-headers-node/graph
   assert-node/graph
   assert-status-node/graph
   config-node/graph
   csv-node/graph
   evaljs-node/graph
   extract-body-node/graph
   extract-header-node/graph
   graphs/skippable-graph
   http-node/graph])

;; ---- Execution ----

(defn sys-time
  "Useful for measuring elapsed time, but the returned millis is NOT a date."
  []
  #?(:cljs (system-time)
     :clj  (System/nanoTime)))

(defn current-time-millis
  "Useful for measuring elapsed time, but the returned millis is NOT a date."
  []
  #?(:cljs (.now js/Date)
     :clj  (System/currentTimeMillis)))

(defn apply-node-to-scope
  [node old-scope]
  (let [start-time (sys-time)
        ; The node's execution function.
        execfunc (:execfunc node)]
    (-> (execfunc node old-scope)
        ;(p/timeout 60000)
        (p/catch
          (fn [error]
            (println "Caught Exception:" error)
            {:node       node
             :id         (graphs/uuid!)
             :start-time start-time
             :end-time   (sys-time)
             :scope      {:apibot|error         true
                          :apibot|timeout-error "timeout"}}))
        (p/then
          (fn [new-scope]
            {:node       node
             :id         (graphs/uuid!)
             :start-time start-time
             :end-time   (sys-time)
             :scope      new-scope})))))

(defn merge-steps
  [scopes initial-scope]
  (let [scope-coll (map :scope scopes)
        merge (apply merge scope-coll)]
    (or merge initial-scope)))


(defn- execute-nodes
  [sorted-nodes initial-scope node-promise-map graph]
  ;; Invariant: all of the first node's predecessors have already
  ;; been executed and their promises are present in the node-promise-map
  ;; sorted-nodes are sorted s.t. for every pair of nodes a,b if a is before b
  ;; then b is not an eventual predecessor of a.
  (if (empty? sorted-nodes)
    (let [promises (vals node-promise-map)]
      (-> (p/all promises)
          (p/then vec)
          (p/then #(sort-by :start-time %))))
    (let [head (first sorted-nodes)
          tail (rest sorted-nodes)
          preds (graphs/predecessors head graph)
          preceding-promises (->> (map :id preds)
                                  (map node-promise-map))
          next-nodes (graphs/successors head graph)]
      (assert (not-any? nil? preceding-promises)
              (str "All predecessors must have a corresponding order. This likely means "
                   "that the sorted-nodes are not being properly sorted."))
      (-> (p/all preceding-promises)
          (p/then vec)
          (p/then #(merge-steps % initial-scope))
          (p/then
            (fn [merged-scope]
              (if (:apibot|error merged-scope)
                (execute-nodes [] initial-scope node-promise-map graph)
                (let [new-step-promise (apply-node-to-scope head merged-scope)
                      new-node-promise-map (assoc node-promise-map
                                             (:id head)
                                             new-step-promise)]
                  (execute-nodes tail initial-scope new-node-promise-map graph)))))))))


(defn execute-once!
  "Graph Execution Semantics:
  1. Executing a graph means iterating the graph until all nodes have been
     executed or until an error is found.
  2. Errors are 'reported' on the :apibot|error key.
  3. Execution is a promise.
  4. The graph is assumed to be well formed.
  5. Nodes which take more than T time to execute will result in a timeout, which
     will halt the execution and result in an error.

  Known error types:
  :apibot|el-error results from a malformed template.
  :apibot|http-error results from an HTTP or Network error.
  :apibot|eval-error results from a malformed eval or runtime error.
  :apibot|assertion-failed results from a failed assertion.
  "
  [initial-scope graphs graph]
  (let [flatenned (->> (graphs/flatten-graph graphs graph)
                       (graphs/append-exit-node (graphs/skippable-node))
                       (graphs/map-nodes
                         (fn [node]
                           (let [id (:graph-id node)
                                 g (graphs/find-graph-by-id id graphs)
                                 execfunc (:execfunc g)]
                             (assert g (str "No graph found with id '" id "'"))
                             (assert execfunc (str "No execfunc found for graph " g))
                             (assoc node :execfunc execfunc)))))]
    (execute-nodes (graphs/sort-nodes-by-predecessor flatenned) initial-scope {} flatenned)))

(defn- create-execution [graph history]
  {:name (graphs/label graph)
   :id (graphs/uuid!)
   :created-at (current-time-millis)
   :graph-id (:id graph)
   :history history})

(defn persist-execution [execution]
  #?(:cljs (do
             (coll/swap-in! *app-state [:execution-history] conj execution)
             (api/insert-execution execution))))


(defn linked-execution-chains
  [execution-index max-execution-count graphs graph]
  (-> (execute-once! {:apibot|row-index execution-index} graphs graph)
      (p/then
        (fn [history]
          (let [execution (create-execution graph history)]
            ; asynchronously insert the execution, don't wait for the result to complete.
            (persist-execution execution)
            (if (exec-history/error? history)
              ; stop immediately if there is an error.
              execution
              ; otherwise continue if the execution count has not reached its limit.
              (let [new-execution-count (inc execution-index)]
                (if (< new-execution-count max-execution-count)
                  (linked-execution-chains new-execution-count max-execution-count
                                           graphs graph)
                  execution))))))))

(defn execute!
  [graphs graph]
  (assert (graphs/dag? graph) "Can only execute DAGs")
  (let [execution-count (csv-node/find-execution-count graph)]
    (linked-execution-chains 0 execution-count graphs graph)))
