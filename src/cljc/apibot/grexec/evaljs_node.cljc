(ns apibot.grexec.evaljs-node
  "Equivalent to eval-node but uses JavaScript instead of ClojureScript."
  (:require
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.executors :as executors]
    [apibot.grexec.eval :as eval]
    [promesa.core :as p]))

;; ---- API ----

(defn execute
  [node scope]
  (let [func (-> node :props :fn eval/evaluate-js-function)]
    (let [result (func scope)]
      (p/promise
        (if (map? result)
          result
          (assoc scope
            :apibot|error true
            :apibot|js-error
            (str "The result of an EvalJS node must always be a non-null javascript object but "
                 "instead returned '" result "'.")))))))


(def graph
  (map->NativeGraph
    {:id       "evaljs"
     :name     "Eval JavaScript"
     :desc     "Evaluates JavaScript"
     :execfunc (executors/wrap-with-try-catch execute executors/on-js-error)
     :spec     nil}))
