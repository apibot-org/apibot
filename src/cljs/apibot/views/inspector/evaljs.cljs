(ns apibot.views.inspector.evaljs
  "An inspector component for evaljs-nodes"
  (:require
    [apibot.grexec.evaljs-node :as evaljs-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons  :refer [form-group-bindable]]
    [reagent.core :refer [cursor]]))

(def editor
  (create-editor
    {:id     "evaljs-editor"
     :class  "form-control"
     :editor {:language :javascript}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn evaljs
  [node-ratom]
  [:form
   [:div.help-block
    [commons/link-docs "evaljs"]]
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   [:div.form-group
    [:label {:for "evaljs-editor" :class "control-label"} "Function"]
    [editor (cursor node-ratom [:props :fn])]]])