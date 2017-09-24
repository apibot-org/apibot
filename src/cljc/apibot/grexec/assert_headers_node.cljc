(ns apibot.grexec.assert-headers-node
  "Performs an assertion over the last request's header."
  (:require
    [#?(:cljs cljs.spec.alpha :clj clojure.spec.alpha) :as s]
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.assertions :as assertions]
    [apibot.grexec.eval :as eval]
    [apibot.grexec.executors :as executors]
    [clojure.string :as string]))

(def doctext
  (string/join "\n"
               ["/**"
                " * An assert-headers function takes the last HTTP"
                " * response's headers and the scope as input and"
                " * and returns a boolean value indicating if"
                " * the assertion passed or not."
                " * "
                " * @return [boolean] true if the assertion passed, false otherwise."
                " */"
                "(headers, scope) => {"
                "  // Replace this with your own assertion logic."
                "  return headers['x-api-token'] !== null"
                "}"]))

;; ---- Spec ----

(s/def ::fn eval/is-js-function?)

(s/def ::template string?)

(s/def ::props
  (s/keys :req [::fn ::template]))

;; ---- API ----

(def graph
  (map->NativeGraph
    {:id            "assert-headers"
     :name          "Assert Headers"
     :desc          "Performs an assertion over the last request's headers."
     :execfunc      (executors/wrap-with-try-catch (assertions/assert-over-http-response :headers) executors/on-js-error)
     :default-props {:fn doctext}
     :spec          nil}))
