(ns apibot.views.code-editor
  "Creates reagent code editor with support for multiple languages."
  (:require
    [reagent.core :as reagent :refer [atom]]
    [apibot.util :as util]))

(defn- find-language [lang]
  (case lang
    :javascript "ace/mode/javascript"
    :clojure "ace/mode/clojure"
    :html "ace/mode/html"
    :xml "ace/mode/xml"
    :json "ace/mode/json"))

(defn set-value [editor text]
  (-> editor (.setValue (or text "") 1)))

(defn create-editor
  "Options:
  - editor: a set of editor options.
    - language: the language's editor.
    - init-value: the editor's initial value.
  - id (required) the editor's ID."
  [opts]
  {:pre [(contains? opts :id)]}
  (let [; Holds a reference to the editor's state.
        *editor (atom nil)
        ; Holds an atom to the Editor's text ratom e.g. **text = (atom *text)
        **text (atom nil)
        editor-opts (:editor opts)]

    (reagent/create-class
      {:should-component-update
       (fn [this old [_ *new-text]]
         (when (not= @@**text @*new-text)
           (reset! **text *new-text)
           (-> @*editor (set-value @*new-text)))
         false)


       :component-did-mount
       (fn [this]
         (let [editor (.edit js/ace (:id opts))
               lang (find-language (:language editor-opts))]
           (-> editor (.setTheme "ace/theme/chrome"))
           (-> editor (.setOptions #js {:maxLines                 (get editor-opts :max-lines 20)
                                        :minLines                 (get editor-opts :min-lines 3)
                                        :readOnly                 (get editor-opts :read-only false)
                                        :autoScrollEditorIntoView true}))
           (-> editor .getSession (.setMode lang))
           (-> editor .getSession (.setTabSize 2))
           (-> editor .getSession (.on "change" (util/throttle 200 #(reset! @**text (.getValue editor)))))
           (-> editor (set-value @@**text))
           (reset! *editor editor)))

       :component-will-unmount
       (fn [this])

       :reagent-render
       (fn [*text]
         (reset! **text *text)
         [:div (dissoc opts :editor)])})))
