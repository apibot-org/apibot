(ns apibot.views.inspector.assert-status
  "An inspector component for assert-status-node"
  (:require
    [apibot.grexec.assert-status-node :as assert-status-node]
    [apibot.views.commons :as commons  :refer [form-group-bindable]]
    [reagent.core :refer [cursor]]))

(defn assert-status
  [node-ratom]
  [:div
   [:h4 "Node " [:span.text-info "Assert Status"]]
   [:p.help-block "Performs an assertion over the last HTTP request's status code."]
   (form-group-bindable
     {:name        "Status From"
      :placeholder "E.g. 200"
      :type        "number"
      :help        "Assert that the status is >= this number"
      :transform   #(if (empty? %) % (js/parseInt %))
      :spec        ::assert-status-node/from}
     (cursor node-ratom [:props :from]))
   (form-group-bindable
     {:name        "Status To"
      :placeholder "E.g. 299"
      :type        "number"
      :help        "Assert that the status is <= this number"
      :transform   #(if (empty? %) % (js/parseInt %))
      :spec        ::assert-status-node/to}
     (cursor node-ratom [:props :to]))])
