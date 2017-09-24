(ns apibot.grexec.assert-node
  (:require
    [#?(:cljs cljs.spec.alpha :clj clojure.spec.alpha) :as s]
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.assertions :as assertions]
    [apibot.grexec.eval :as eval]
    [apibot.grexec.executors :as executors]
    [clojure.string :as string]
    [promesa.core :as p]))

(def doctext
  (string/join "\n"
               ["/**"
                " * An assert function takes the scope as input"
                " * and returns a boolean value indicating if"
                " * the assertion passed or not."
                " * "
                " * @return [boolean] true if the assertion passed, false otherwise."
                " */"
                "(scope) => {"
                "  // Replace this with your own assertion logic."
                "  return scope.user !== null;"
                "}"]))

;; ---- Specs ----

(s/def ::template (s/and string? (complement empty?)))
(s/def ::fn eval/is-js-function?)

;; ---- API ----

(defn execute
  [node scope]
  (let [{:keys [fn template]} (:props node)
        func (-> fn eval/evaluate-js-function)
        rendered-template (el/render-str template scope)]
    (if (func scope)
      (p/promise scope)
      (p/promise
        (assoc scope
          :apibot|error true
          :apibot|assertion-failed @rendered-template)))))

(def graph
  (map->NativeGraph
    {:id            "assert"
     :name          "Assert"
     :desc          "Performs an assertion"
     :execfunc      (executors/wrap-with-try-catch execute executors/on-js-error)
     :default-props {:fn doctext}
     :spec          nil}))
