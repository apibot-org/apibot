(ns apibot.views.inspector.assert-body
  "An inspector component for assert-body-node"
  (:require
    [apibot.grexec.assert-body-node :as assert-body-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons :refer [form-group-bindable]]
    [reagent.core :refer [cursor]]
    [clojure.string :as string]
    [apibot.coll :as coll]))



(def editor
  (create-editor
    {:id     "body-editor"
     :class  "form-control"
     :editor {:language :javascript}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn assert-body
  [node-ratom]
  [:div
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   (form-group-bindable
     {:name        "Error Template"
      :help        "The message to display in case the assertion fails."
      :spec        ::assert-body-node/template}
     (cursor node-ratom [:props :template]))
   [:div.form-group
    [:label {:for "eval-editor" :class "control-label"} "Function"]
    [editor (cursor node-ratom [:props :fn])]]])
