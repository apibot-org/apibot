(ns apibot.views.inspector.eval
  "An inspector component for eval-nodes"
  (:require
    [apibot.grexec.eval-node :as eval-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :refer [form-group-bindable]]
    [reagent.core :refer [cursor]]))

(def editor
  (create-editor
    {:id     "eval-editor"
     :class  "form-control"
     :editor {:language :clojure}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn eval
  [node-ratom]
  [:form
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   [:div.form-group
    [:label {:for "eval-editor" :class "control-label"} "Function"]
    [editor (cursor node-ratom [:props :fn])]
    [:p.help-block
     "Define a function in the global namespace which takes a"
     "scope as argument and returns a scope. Example:"
     [:br]
     [:code "(fn [scope] (assoc scope :happy true))"]]]])
