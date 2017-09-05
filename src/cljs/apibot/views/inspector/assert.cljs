(ns apibot.views.inspector.assert
  "An inspector component for assert-node"
  (:require
    [apibot.grexec.assert-node :as assert-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons :refer [form-group-bindable]]
    [clojure.string :as string]
    [reagent.core :refer [cursor]]
    [apibot.coll :as coll]))

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
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   (form-group-bindable
     {:name        "Error Message"
      :placeholder "E.g. expected user to be present but wasn't."
      :spec        ::assert-node/template
      :help        "The message to display in case the assertion fails."}
     (cursor node-ratom [:props :template]))
   [:div.form-group
    [:label {:for "eval-editor" :class "control-label"} "Function"]
    [editor (cursor node-ratom [:props :fn])]]])
