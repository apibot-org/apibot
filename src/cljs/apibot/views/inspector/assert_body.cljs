(ns apibot.views.inspector.assert-body
  "An inspector component for assert-body-node"
  (:require
    [apibot.grexec.assert-body-node :as assert-body-node]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons :refer [form-group-bindable]]
    [clojure.string :refer [join]]
    [reagent.core :refer [cursor]]))

(def sample
  (join "\n"
        ["/**"
         " * Define a function in the global namespace"
         " * @param body the last HTTP request's body as a map."
         " * @param scope the scope"
         " * @return a boolean value indicating wether the assertion passed or failed."
         " */"
         "(body, scope) => {"
         "  return body.user_id != scope.user_id;"
         "}"]))

(def editor
  (create-editor
    {:id     "body-editor"
     :class  "form-control"
     :editor {:language :javascript}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn assert-body
  [node-ratom]
  [:form
   [:div.help-block
    [commons/link-docs "assert-body"]]
   (form-group-bindable
     {:name "Name"}
     (cursor node-ratom [:name]))
   (form-group-bindable
     {:name        "Error Template"
      :placeholder "Expected :happy to be present but was '${happy}' instead."
      :spec        ::assert-body-node/template}
     (cursor node-ratom [:props :template]))
   [:div.form-group
    [:label {:for "eval-editor" :class "control-label"} "Function"]
    [editor (cursor node-ratom [:props :fn])]]])
