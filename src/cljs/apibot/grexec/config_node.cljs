(ns apibot.grexec.config-node
  (:require
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.util :refer [key-val-headers->map keywordize-keys]]
    [cljs.spec.alpha :as s]
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
    (merge scope (-> node :props :config key-val-headers->map keywordize-keys))))

(def graph
  (map->NativeGraph
    {:id       "config"
     :name     "Config"
     :desc     "Defines environment variables"
     :execfunc execute
     :spec     nil}))
