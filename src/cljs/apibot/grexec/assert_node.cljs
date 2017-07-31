(ns apibot.grexec.assert-node
  (:require
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.util :as util]
    [promesa.core :as p]))

;; ---- API ----

(defn execute
  [node scope]
  (let [{:keys [fn template]} (:props node)
        func (-> fn util/evaluate-js-function)
        rendered-template (el/render-str template scope)]
    (if (func scope)
      (p/promise scope)
      (p/promise
        (assoc scope
          :apibot.error true
          :apibot.assertion-failed @rendered-template)))))

(def graph
  (map->NativeGraph
    {:id       "assert"
     :name     "Assert"
     :desc     "Performs an assertion"
     :execfunc execute
     :spec     nil}))
