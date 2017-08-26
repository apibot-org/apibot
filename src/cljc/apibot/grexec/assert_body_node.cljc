(ns apibot.grexec.assert-body-node
  "Performs an assertion over the last request's body."
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.assertions :as assertions]
    [apibot.grexec.executors :as executors]
    [apibot.grexec.eval :as eval]))

;; ---- Spec ----

(s/def ::fn eval/is-js-function?)

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
