(ns apibot.grexec.extract-header-node
  "Extracts a header from the last request."
  (:require
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.util :as util]
    [cljs.spec.alpha :as s]
    [promesa.core :as p]))

;; ---- Spec ----

(s/def ::name string?)
(s/def ::header string?)

(s/def ::props
  (s/keys :req [::name ::header]))

;; ---- API ----

(defn execute
  [node scope]
  (let [key-name (-> node :props :name)
        header-key (-> node :props :header keyword)
        header-val (get-in scope [:apibot.http-response :headers header-key])]
    (p/promise (assoc scope key-name header-val))))

(def graph
  (map->NativeGraph
    {:id       "extract-header"
     :name     "Extract Header"
     :desc     "Extracts the header with the given name."
     :execfunc execute
     :spec     nil}))
