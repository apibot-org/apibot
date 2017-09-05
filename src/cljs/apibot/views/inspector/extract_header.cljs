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
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   (form-group-bindable
     {:name "Header Name"
      :spec ::extract-header-node/header
      :help "Enter the name of the HTTP header you want to extract e.g. x-api-token."}
     (cursor node-ratom [:props :header]))
   (form-group-bindable
     {:name "Property Name"
      :spec ::extract-header-node/name
      :help "Enter the name you want to give to the extracted header in the Scope."}
     (cursor node-ratom [:props :name]))])

