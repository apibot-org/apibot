(ns apibot.views.inspector.assert-headers
  "An inspector component for assert-headers-node"
  (:require
    [apibot.grexec.assert-body-node :as assert-body-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons :refer [form-group-bindable]]
    [reagent.core :refer [cursor]]
    [clojure.string :as string]
    [apibot.coll :as coll]))


(def editor
  (create-editor
    {:id     "headers-editor"
     :class  "form-control"
     :editor {:language :javascript}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn assert-headers
  [*node]
  [:div
   [:h4 "Node " [:span.text-info "Assert Headers"]]
   [:p.help-block "Performs an assertion over the last HTTP request's headers."]
   (form-group-bindable
     {:name "Name"}
     (cursor *node [:name]))
   (form-group-bindable
     {:name        "Error Message"
      :help        "The message to display in case the assertion fails."
      :spec        ::assert-body-node/template}
     (cursor *node [:props :template]))
   [:div.form-group
    [:label {:for "eval-editor" :class "control-label"} "Function"]
    [editor (cursor *node [:props :fn])]]])
