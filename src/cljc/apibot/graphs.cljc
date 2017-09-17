(ns apibot.graphs
  "Common functions that deal with graphs."
  (:require
    [#?(:cljs cljs.spec.alpha :clj clojure.spec.alpha) :as spec]
    [apibot.coll :as coll]
    [apibot.nodes :as nodes]
    [clojure.set :as sets]
    [clojure.string :as s :refer [join]]
    [promesa.core :as p]))


;; ---- Specs ----

(spec/def ::id coll/non-empty-string?)
(spec/def ::source coll/non-empty-string?)
(spec/def ::target coll/non-empty-string?)
(spec/def ::name string?)
(spec/def ::desc string?)
(spec/def ::executable boolean?)
(spec/def ::projects (spec/every string? :kind set?))
(spec/def ::node map?)
(spec/def ::nodes (spec/and (spec/every ::node :kind vector?)))
(spec/def ::edge (spec/keys :req-un [::id ::source ::target]))
(spec/def ::edges (spec/and (spec/every ::edge :kind vector?)))

(spec/def ::custom-graph
  (spec/keys :req-un
             [::id
              ::name
              ::desc
              ::executable
              ::projects
              ::nodes
              ::edges]))

;; ---- End Specs ----

(declare flatten-graph)
(declare find-graph-by-id)
(declare graph->node)
(declare find-start-node)
(declare singleton?)

;; ---- PROTOCOLS ----

(defprotocol Graph
  (editable? [this])
  (kind [this])
  (label [this]))

(defrecord NativeGraph
  [id name desc execfunc spec default-props]
  Graph
  (editable? [this] false)
  (kind [this] id)
  (label [this] name))

(defrecord CustomGraph
  [id name desc executable projects nodes edges]
  Graph
  (editable? [this] true)
  (kind [this] "custom")
  (label [this]
    (cond
      (not (empty? (:name this)))
      (:name this)

      (singleton? this)
      (let [node (find-start-node this)]
        (nodes/label node ""))

      :else
      "")))

(spec/fdef map->CustomGraph
           :args (spec/cat :map ::custom-graph))

(defn create-custom-graph [{:keys [id name desc executable projects nodes edges]}]
  (map->CustomGraph
    {:id         id
     :name       name
     :desc       desc
     :executable executable
     :projects   (set projects)
     :nodes      (vec nodes)
     :edges      (vec edges)}))


; ---- END PROTOCOLS ----

(defn count-nodes [graph] (count (:nodes graph)))
(defn count-edges [graph] (count (:edges graph)))

(defn uuid! []
  "Generate a large random string."
  (str #?(:cljs (random-uuid)
          :clj  (java.util.UUID/randomUUID))))

(defn make-edge [source-id target-id]
  {:id     (uuid!)
   :source source-id
   :target target-id})

(defn connect
  "Adds an edge from node-source to node-target."
  [node-source node-target graph]
  (update graph :edges conj (make-edge (:id node-source) (:id node-target))))

(defn connect-from
  "Connects all nodes in nodes-from to node-target"
  [nodes-from node-target graph]
  (reduce (fn [g node-source]
            (connect node-source node-target g))
          graph
          nodes-from))

(defn connect-to
  "Connects node-source to all nodes in nodes-targets."
  [node-source node-targets graph]
  (reduce (fn [g node-target]
            (connect node-source node-target g))
          graph
          node-targets))

(defn conj-node
  "Adds the given node to the graph."
  [node graph]
  (update graph :nodes conj node))

(defn conj-edge
  "Adds the given node to the graph."
  [edge graph]
  (update graph :edges conj edge))

(defn conj-nodes
  "Adds the given nodes to the graph"
  [nodes graph]
  (reduce (fn [g node]
            (conj-node node g))
          graph
          nodes))

(defn conj-edges
  "Adds the given edges to the graph"
  [edges graph]
  (reduce (fn [g edge]
            (conj-edge edge g))
          graph
          edges))

(defn edges-from
  "Finds all edges that originate from node."
  [node graph]
  (filter (fn [{:keys [source]}]
            (= source (:id node)))
          (:edges graph)))

(defn edges-to
  "Finds all edges that terminate in node."
  [node graph]
  (filter (fn [{:keys [target]}]
            (= target (:id node)))
          (:edges graph)))

(defn find-node-by-id
  "Finds a node given its ID."
  [node-id graph]
  (->> (:nodes graph)
       (filter #(= node-id (:id %)))
       (first)))

(defn successors
  "Finds the successors of a node."
  [node graph]
  (map (fn [{:keys [target]}]
         (find-node-by-id target graph))
       (edges-from node graph)))

(defn predecessors
  "Finds the predecessors of a node."
  [node graph]
  (map (fn [{:keys [source]}]
         (find-node-by-id source graph))
       (edges-to node graph)))

(defn find-leaves
  "Finds all nodes without successors"
  [graph]
  (filter (fn [node]
            (empty? (successors node graph)))
          (:nodes graph)))

(defn duplicate-graph
  [graph]
  ;; Since node IDs need only be unique inside the graph, we can leave
  ;; them intact. TODO is this still true?
  (assoc graph :id (uuid!)
               :name (str (label graph) " - COPY")))

(defn merge-graph
  "Merges the origin-graph onto the onto-graph, which means
  all nodes and edges in origin-graph are copied over to onto-graph."
  [gen-id origin-graph onto-graph]
  (let [assoc-id (fn [node]
                   (assoc node :id (gen-id (:id node))))
        new-nodes (map assoc-id (:nodes origin-graph))
        new-edges (map (fn [{:keys [id source target]}]
                         {:id     (gen-id id)
                          :source (gen-id source)
                          :target (gen-id target)})
                       (:edges origin-graph))]
    (->> onto-graph
         (conj-nodes new-nodes)
         (conj-edges new-edges))))

(defn with-executable
  "Given a boolean executable and a graph, makes the given graph executable (or not)"
  [executable graph]
  (assoc graph :executable executable))

(defn with-project [project-id graph]
  (update-in graph [:projects] conj project-id))

(defn with-nodes [nodes graph]
  (assoc graph :nodes (vec nodes)))

(defn find-start-nodes
  "Finds all starting nodes in the graph"
  [graph]
  (->> (:nodes graph)
       (filter (fn [node] (empty? (predecessors node graph))))))

(defn find-start-node
  "Finds the starting node (or nil if not found)"
  [graph]
  (first (find-start-nodes graph)))

(defn traverse
  [starting-node successors-fn graph]
  (loop [result (transient [])
         visited #{}
         next-nodes (list starting-node)]
    (if (empty? next-nodes)
      (persistent! result)
      (let [head (first next-nodes)
            tail (rest next-nodes)]
        (if (visited head)
          (recur result visited tail)
          (recur (conj! result head)
                 (conj visited head)
                 (into tail (successors-fn head graph))))))))

(defn connected-component
  [node graph]
  (->> (concat (traverse node successors graph)
               (traverse node predecessors graph))
       (set)))

(defn connected-components
  "Returns the graph's connected components. This function
  returns a vector where each item is a seq of nodes."
  [graph]
  (loop [result []
         visited-nodes #{}
         remaining-nodes (:nodes graph)]
    (if (empty? remaining-nodes)
      result
      (let [head (first remaining-nodes)
            tail (rest remaining-nodes)]
        (if (visited-nodes head)
          (recur result visited-nodes tail)
          (let [subgraph (connected-component head graph)]
            (recur (conj result subgraph)
                   (into visited-nodes subgraph)
                   tail)))))))

(defn loopless?
  "Returns true iff the graph contains no loops"
  ([graph]
   (every? #(loopless? % graph) (:nodes graph)))
  ([starting-node graph]
   (loop [visited #{starting-node}
          next-nodes (successors starting-node graph)]
     (if (empty? next-nodes)
       true
       (let [head (first next-nodes)
             tail (rest next-nodes)]
         (if (= starting-node head)
           false
           (if (visited head)
             (recur visited tail)
             (recur (conj visited head)
                    (into tail (successors head graph))))))))))


(defn empty-graph
  "Creates a new empty graph."
  []
  (create-custom-graph
    {:id         (uuid!)
     :name       ""
     :desc       ""
     :projects   (set [])
     :executable false
     :nodes      []
     :edges      []}))

(def skippable-graph
  (map->NativeGraph
    {:id       "skippable"
     :name     "Skippable Node"
     :desc     "Does nothing"
     :execfunc (fn [node scope] (p/promise scope))
     :spec     nil}))

(defn skippable-node []
  (graph->node skippable-graph))


(defn dag? [graph]
  (let [start-nodes (find-start-nodes graph)
        start-node (first start-nodes)]
    (if (not= (count start-nodes) 1)
      false
      (loop [remaining-nodes [start-node]
             visited-node-ids #{}]
        (if (empty? remaining-nodes)
          (= (count visited-node-ids) (count (:nodes graph)))
          (let [head (first remaining-nodes)]
            (if (contains? visited-node-ids (:id head))
              false
              (recur (concat (rest remaining-nodes) (successors head graph))
                     (conj visited-node-ids (:id head))))))))))


(defn executable?
  "Determines if the given graph is in an executable state."
  [graph]
  (and
    ; The graph must be explicitly marked as executable.
    (:executable graph)
    (dag? graph)))


(defn- edge->cyto [edge]
  {:group "edges"
   ;; data expects an {id, source, target}
   :data  edge})

(defn graph->cytoscape
  "Converts an apibot graph to a cytoscape graph"
  [graph]
  (->>
    (concat (map nodes/node->cyto (:nodes graph))
            (map edge->cyto (:edges graph)))
    (into [])))

(defn cyto->node [{:keys [data position]}]
  (let [{:keys [node text id]} data]
    (assoc node
      :name text
      :position position)))

(defn- cyto->edge [edge]
  (:data edge))

(defn cytoscape->graph
  [{:keys [nodes edges]} graph]
  (let [new-nodes (map cyto->node nodes)
        new-edges (map cyto->edge edges)]
    (assoc graph
      :edges (into [] new-edges)
      :nodes (into [] new-nodes))))


(defn graph->node
  "Instantiates a graph, returning a graph node."
  [graph]
  {:id       (uuid!)
   :type     (kind graph)
   :graph-id (:id graph)
   :name     (label graph)
   :position nil
   :selected false
   :spec     (:spec graph)
   :props    (get graph :default-props {})})

(defn append-exit-node
  "Appending an exit node means adding a node to the graph s.t. all leaf nodes
  terminate there. If there is only one leaf, this function returns the graph.

  NOTE: since append-exit-node may or may not add a note, its always a good idea
  to use find-leaves to get the exit-node."
  [node graph]
  (let [leaves (find-leaves graph)]
    (if (= 1 (count leaves))
      graph
      (reduce (fn [g leaf]
                (connect leaf node g))
              (conj-node node graph)
              leaves))))

(defn remove-edges-by
  "Removes all edges that match the given predicate"
  [predicate graph]
  (let [new-edges
        (->> (filter (complement predicate) (:edges graph))
             (into []))]
    (assoc graph :edges new-edges)))

(defn remove-nodes-by
  "Removes all nodes that match the given predicate and all edges the connected
  from or to those nodes."
  [predicate graph]
  (let [;; The nodes that match and those that don't match the predicate
        {to-remove true to-keep false} (group-by predicate (:nodes graph))

        ;; the ids of the nodes that will be removed
        node-ids-to-remove (set (map :id to-remove))

        ;; a new graph with the edges removed
        new-graph (remove-edges-by
                    (fn [{:keys [source target]}]
                      (or (node-ids-to-remove source)
                          (node-ids-to-remove target)))
                    graph)]

    ;; Finally return the new-graph without the nodes that are meant to be removed
    (assoc new-graph :nodes to-keep)))

(defn remove-nodes-by-id
  "Removes a node by ID."
  [id graph]
  (remove-nodes-by #(= id (:id %)) graph))

(defn map-nodes
  "Maps the nodes in the graph"
  [f graph]
  (let [new-nodes
        (->> (:nodes graph)
             (map f)
             (into []))]
    (assoc graph :nodes new-nodes)))


(defn find-edges
  [predicate graph]
  (filter predicate (:edges graph)))

(defn- expand-inner-graph
  [expandable-node inner-graph graphs graph]
  (let [;; A function which assigns random ids to x but always returns the same
        ;; id given an input.
        gen-id (memoize (fn [x] (uuid!)))

        ;; Finds the predecessors and successors
        from-nodes (predecessors expandable-node graph)
        to-nodes (successors expandable-node graph)

        ;; Initialize a new skippable node
        exit-node (skippable-node)

        ;; Append an exit node to the inner graph
        inner-graph (map-nodes
                      (fn [node]
                        node
                        (update node :name #(str (:name expandable-node) " > " %)))
                      inner-graph)
        inner-graph-with-exit-node (append-exit-node exit-node inner-graph)

        ;; Find the head of the inner graph
        inner-starting-node (find-start-node inner-graph-with-exit-node)

        ;; Note that some IDs have changed in the new graph.
        new-graph (->> graph
                       (remove-nodes-by-id (:id expandable-node))
                       (merge-graph gen-id inner-graph-with-exit-node))

        ;; Since IDs have changed, we need to reaload the starting node.
        new-starting-node (find-node-by-id
                            (gen-id (:id inner-starting-node))
                            new-graph)

        ;; Since IDs have changed AND and the exit node
        ;; might not have been added, we need to reaload the exit node.
        new-exit-node (-> (find-leaves inner-graph-with-exit-node)
                          (first)
                          (:id)
                          (gen-id)
                          (find-node-by-id new-graph))]

    (->> new-graph
         (connect-from from-nodes new-starting-node)
         (connect-to new-exit-node to-nodes))))

(defn flatten-graph
  [graphs graph]
  (reduce (fn [graph node]
            (if (not= (:type node) "custom")
              graph
              (let [inner-graph (find-graph-by-id (:graph-id node) graphs)
                    inner-graph (flatten-graph graphs inner-graph)]
                (expand-inner-graph node inner-graph graphs graph))))
          graph
          (:nodes graph)))

(defn singleton?
  "Returns true iff the graph has a single node (and therefore no edges)"
  [graph]
  (= 1 (count (:nodes graph))))

(defn sort-nodes-by-predecessor
  "Returns a vector of nodes sorted by their execution order."
  [graph]
  (if (= 0 (count-nodes graph))
    []                                                      ; if the graph is empty, exit immediately.
    (loop [result []
           visited #{}
           remaining-nodes [(find-start-node graph)]]
      (if (empty? remaining-nodes)
        result
        (let [head (first remaining-nodes)
              tail (into [] (rest remaining-nodes))
              preds (predecessors head graph)]
          (cond
            ;; if the node has already been visited, skip it. It is already included
            ;; in the results.
            (visited head)
            (recur result
                   visited
                   tail)

            ;; If every predecessor has been visited before, then we can add the node.
            (every? visited preds)
            (recur (conj result head)
                   (conj visited head)
                   (into tail (successors head graph)))

            ;; Finally, if there is at least one predecessor that has nod been added,
            ;; re-queue the head at the end of the queue.
            :else
            (recur result
                   visited
                   (conj tail head))))))))


(defn in-project?
  "Determines if the given graph is part of the given project"
  [project-id graph]
  (cond
    (not (editable? graph))
    true
    (= project-id "default")
    true
    :else
    (contains? (:projects graph) project-id)))


(defn matches-query?
  [query graph]
  (let [name (s/lower-case (or (label graph) ""))
        desc (s/lower-case (or (:desc graph) ""))]
    (or (empty? query)
        (s/includes? name query)
        (s/includes? desc query))))


(defn group-nodes-by-height
  "Returns a map of {height [nodes]} where height => [nodes] means that all nodes
  have the given height (relative to the 'start-node')."
  [graph]
  (let [find-height (memoize
                      (fn height [node]
                        (let [preds (predecessors node graph)]
                          (if (empty? preds)
                            0
                            (+ 1 (apply max (map height preds)))))))]
    (group-by find-height (:nodes graph))))


(defn sort-nodes-by-family [nodes graph]
  (sort-by (fn [node]
             (let [preds (predecessors node graph)
                   x (->> preds
                          (map :position)
                          (map :x)
                          (apply min))]
               (into [x] (map :id preds))))
           nodes))


(defn prettify
  "Makes this graph pretty."
  [graph]
  (if (<= (count-nodes graph) 1)
    ;; If the graph has zero or 1 nodes it is always formatted
    graph
    (let [dist-x 100
          dist-y 45
          start-node (find-start-node graph)
          nodes-by-height (group-nodes-by-height graph)
          widest-level (apply max (keys nodes-by-height))
          formatted-nodes
          (mapcat (fn [[level nodes-in-level]]
                    (let [pad (/ (* dist-x
                                    (- widest-level
                                       (count nodes-in-level)))
                                 2)]
                      (map-indexed (fn [i node]
                                     (-> (assoc-in node [:position :x] (+ pad (* i dist-x)))
                                         (assoc-in [:position :y] (* dist-y level))))
                                   (sort-nodes-by-family nodes-in-level graph))))
                  nodes-by-height)]
      (with-nodes formatted-nodes graph))))

(spec/fdef prettify
           :args (spec/cat :graph (spec/and ::custom-graph dag?)))

(defn contains-nodes-by-graph?
  "Returns true if there is a node in graph that was instantiated from graph-id"
  [graph-id graph]
  (->> (:nodes graph)
       (filter #(= (:graph-id %) graph-id))
       (empty?)
       (not)))


;; ---- Graphs (plural) ----

(defn find-usages
  "Searches the given graphs for nodes that were instantiated from the given graph.
  Returns all graphs that use the given graph-id."
  [graph-id graphs]
  (filter #(contains-nodes-by-graph? graph-id %) graphs))

(defn map-graph-if
  [filter-func map-func graphs]
  (->> graphs
       (map #(if (filter-func %)
               (map-func %)
               %))
       (into [])))

(defn update-graph
  "Updates the graph that matches graph's :id.
  Returns a vector of graphs."
  [graph graphs]
  (map-graph-if #(= (:id %) (:id graph))
                (fn [_] graph)
                graphs))

(defn find-graph-by-id
  "Finds a graph with the given ID."
  [id graphs]
  (->> graphs
       (filter #(= (:id %) id))
       (first)))

(defn remove-graph [graph graphs]
  "Returns all graphs except the given graph"
  (->> graphs
       (filter (fn [g] (not= (:id g) (:id graph))))
       (into [])))

(defn find-removed
  "Given two snapshots of the graphs state, determines which graphs
   were removed.
   Returns the IDs of the removed graphs."
  [old-graphs new-graphs]
  (let [new-ids (set (map :id new-graphs))]
    (->> (map :id old-graphs)
         (filter #(not (contains? new-ids %))))))

(defn find-updated
  "Given two snapshots of the graphs state, determines which graphs
   were updated.
   Returns the updated graphs"
  [old-graphs new-graphs]
  (let [old-graphs-by-id (group-by :id old-graphs)]
    (filter (fn [new]
              (not= new (first (get old-graphs-by-id (:id new)))))
            new-graphs)))

