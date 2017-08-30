(ns apibot.grexec.extract-body-node
  "Equivalent to eval-node but uses JavaScript instead of ClojureScript."
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.eval :as eval]
    [promesa.core :as p]))

;; ---- Spec ----

(s/def ::fn eval/is-js-function?)
(s/def ::name keyword?)

(s/def ::props
  (s/keys :req [::fn ::name]))

;; ---- API ----

(defn execute
  [node scope]
  (let [key-name (-> node :props :name)
        func (-> node :props :fn eval/evaluate-js-function)
        body (get-in scope [:apibot|http-response :body])
        value (func body)]
    (p/promise (assoc scope key-name value))))

(def graph
  (map->NativeGraph
    {:id       "extract-body"
     :name     "Extract Body"
     :desc     "Extracts the value with the given path"
     :execfunc execute
     :spec     nil}))
