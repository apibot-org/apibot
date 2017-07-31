(ns apibot.views.inspector.extract-header
  "An inspector component for extract-body nodes."
  (:require
    [apibot.grexec.extract-header-node :as extract-header-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons  :refer [form-group-bindable]]
    [reagent.core :refer [cursor]]))

(def editor
  (create-editor
    {:id     "extract-header-node-editor"
     :class  "form-control"
     :editor {:language :javascript}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn extract-header
  [node-ratom]
  [:form
   [:div.help-block
    [commons/link-docs "extract-header"]]
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   (form-group-bindable
     {:name "Property Name"
      :spec ::extract-header-node/name}
     (cursor node-ratom [:props :name]))
   (form-group-bindable
     {:name "Header Name"
      :spec ::extract-header-node/header}
     (cursor node-ratom [:props :header]))])
