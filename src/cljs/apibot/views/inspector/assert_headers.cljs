(ns apibot.views.inspector.assert-headers
  "An inspector component for assert-headers-node"
  (:require
    [apibot.grexec.assert-body-node :as assert-body-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons  :refer [form-group-bindable]]
    [clojure.string :refer [join]]
    [reagent.core :refer [cursor]]))

(def sample
  (join "\n"
        ["/**"
         " * Define a function in the global namespace"
         " * @param headers the last HTTP request's headers as a map."
         " * @param scope the scope"
         " * @return a boolean value indicating wether the assertion passed or failed."
         " */"
         "(headers, scope) => {"
         "  return headers.auth_token != null;"
         "}"]))

(def editor
  (create-editor
    {:id     "headers-editor"
     :class  "form-control"
     :editor {:language :javascript}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn assert-headers
  [*node]
  [:form
   [:div.help-block
    [commons/link-docs "assert-headers"]]
   (form-group-bindable
     {:name "Name"}
     (cursor *node [:name]))
   (form-group-bindable
     {:name        "Error Template"
      :placeholder "Expected :happy to be present but was '${happy}' instead."
      :spec        ::assert-body-node/template}
     (cursor *node [:props :template]))
   [:div.form-group
    [:label {:for "eval-editor" :class "control-label"} "Function"]
    [editor (cursor *node [:props :fn])]]])
