(ns apibot.grexec.assert-headers-node
  "Performs an assertion over the last request's header."
  (:require
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.assertions :as assertions :refer [expected-http-response-error]]
    [apibot.util :as util]
    [cljs.spec.alpha :as s]
    [promesa.core :as p]))

;; ---- Spec ----

(s/def ::fn util/is-js-function?)

(s/def ::template string?)

(s/def ::props
  (s/keys :req [::fn ::template]))

;; ---- API ----

(def graph
  (map->NativeGraph
    {:id       "assert-headers"
     :name     "Assert Headers"
     :desc     "Performs an assertion over the last request's headers."
     :execfunc (assertions/assert-over-http-response :headers)
     :spec     nil}))
