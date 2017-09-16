(ns apibot.views.inspector.extract-body
  "An inspector component for extract-body nodes."
  (:require
    [apibot.grexec.extract-body-node :as extract-body-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons :refer [form-group-bindable]]
    [clojure.string :refer [join]]
    [reagent.core :refer [cursor]]
    [clojure.string :as string]
    [apibot.coll :as coll]))

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
   [:h4 "Node " [:span.text-info "Extract Body"]]
   [:p.help-block "Extracts a value from the last request's HTTP body"]
   (form-group-bindable
     {:name "Property Name"
      :spec ::extract-body-node/name
      :help "Enter the name that the extracted property will have in the Scope."}
     (cursor node-ratom [:props :name]))
   [:div.form-group
    [:label {:for "extract-body-node-editor" :class "control-label"} "Function"]
    [editor (cursor node-ratom [:props :fn])]]])
