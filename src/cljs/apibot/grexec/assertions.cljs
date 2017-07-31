(ns apibot.grexec.assertions
  "A namespace that exposes common functions for assertions"
  (:require
    [apibot.el :as el]
    [apibot.util :as util]
    [promesa.core :as p]))

(defn expected-http-response-error
  [scope]
  (assoc scope
    :apibot.error true
    :apibot.assertion-failed
    (str "No HTTP response was found. This node may only be used to assert the\n"
         "contents of an HTTP response. Did you maybe forget to create an HTTP\n"
         "Request node before this assertion?")))

(defn assert-over-http-response
  [path]
  (fn [node scope]
    (let [{:keys [template]} (:props node)
          body (-> scope :apibot.http-response path)
          func (-> node :props :fn util/evaluate-js-function)
          rendered-template (el/render-str template body)]
      (cond
        (not body)
        (p/promise (expected-http-response-error scope))

        (func body scope)
        (p/promise scope)

        :else
        (p/promise
          (assoc scope
            :apibot.error true
            :apibot.assertion-failed @rendered-template))))))
