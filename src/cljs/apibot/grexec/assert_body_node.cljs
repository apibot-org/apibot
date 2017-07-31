(ns apibot.grexec.assert-body-node
  "Performs an assertion over the last request's body."
  (:require
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.assertions :as assertions :refer [expected-http-response-error]]
    [apibot.grexec.executors :as executors]
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
    {:id       "assert-body"
     :name     "Assert Body"
     :desc     "Performs an assertion over the last request's body."
     :execfunc (executors/wrap-with-try-catch
                 (assertions/assert-over-http-response :body)
                 executors/on-js-error)
     :spec     nil}))
