(ns apibot.grexec.eval-node
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [cljs.js :refer [empty-state eval js-eval]]
    [cljs.reader :refer [read-string]]
    [promesa.core :as p]))

(defn eval-str [s]
  (eval (empty-state)
        (read-string s)
        {:eval       js-eval
         :source-map true
         :context    :expr}
        (fn [result] result)))

;; ---- Spec ----

(s/def ::fn
  (s/and
    string?
    (fn [string]
      (try
        (let [evaled (eval-str string)]
          (fn? (:value evaled)))
        (catch js/Object e
          false)))))

(s/def ::props
  (s/keys :req [::fn]))

;; ---- API ----

(defn execute
  [node scope]
  (let [func (-> node :props :fn eval-str :value)]
    (p/promise (func scope))))

(def graph
  (map->NativeGraph
    {:id       "eval"
     :name     "Eval"
     :desc     "Evaluates ClojureScript"
     :execfunc execute
     :spec     nil}))
