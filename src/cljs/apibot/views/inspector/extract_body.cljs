(ns apibot.views.inspector.extract-body
  "An inspector component for extract-body nodes."
  (:require
    [apibot.grexec.extract-body-node :as extract-body-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons  :refer [form-group-bindable]]
    [reagent.core :refer [cursor]]))

(def editor
  (create-editor
    {:id     "extract-body-node-editor"
     :class  "form-control"
     :editor {:language :javascript}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn extract-body
  [node-ratom]
  [:form
   [:div.help-block
    [commons/link-docs "extract-body"]]
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   (form-group-bindable
     {:name "Property Name"}
     (cursor node-ratom [:props :name]))
   [:div.form-group
    [:label {:for "extract-body-node-editor" :class "control-label"} "Function"]
    [editor (cursor node-ratom [:props :fn])]]])
