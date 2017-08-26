(ns apibot.grexec.config-node
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.coll :refer [key-vals->map keywordize-keys]]
    [promesa.core :as p]))

;; ---- Spec ----

(s/def ::key string?)
(s/def ::val string?)
(s/def ::prop (s/keys :req [::key ::val]))
(s/def ::config (s/* ::prop))
(s/def ::props (s/keys :req [::config]))

;; ---- API ----

(defn execute
  [node scope]
  (p/promise
    (merge scope (-> node :props :config key-vals->map keywordize-keys))))

(def graph
  (map->NativeGraph
    {:id       "config"
     :name     "Config"
     :desc     "Defines environment variables"
     :execfunc execute
     :spec     nil}))
