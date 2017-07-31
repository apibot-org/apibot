(ns apibot.views.inspector.assert
  "An inspector component for assert-node"
  (:require
    [apibot.grexec.assert-node :as assert-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons  :refer [form-group-bindable]]
    [reagent.core :refer [cursor]]))

;
(def editor
  (create-editor
    {:id     "body-editor"
     :class  "form-control"
     :editor {:language :javascript}
     :style  {:padding "0px"
              :height  "250px"}}))

;; XXX can't use the name 'assert' because its a core macro, jammer.
(defn assert-view
  [node-ratom]
  [:form
   [:div.help-block
    [commons/link-docs "assert"]]
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   (form-group-bindable
     {:name        "Error Template"
      :placeholder "Expected :happy to be present but was '${happy}' instead."
      :spec        string?}
     (cursor node-ratom [:props :template]))
   [:div.form-group
    [:label {:for "eval-editor" :class "control-label"} "Function"]
    [editor (cursor node-ratom [:props :fn])]]])
