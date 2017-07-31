(ns apibot.views.code-editor
  "Creates reagent code editor with support for multiple languages."
  (:require
    [reagent.core :as reagent :refer [atom]]))

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
    - language: the language's editor
  - id (required) the editor's ID."
  [opts]
  {:pre [(contains? opts :id)]}
  (let [editor-ratom (atom nil)
        ;; XXX an (atom (atom text))
        text-ratom (atom nil)
        editor-opts (:editor opts)]

    (reagent/create-class
      {:should-component-update
       (fn [this old [_ new-text-ratom]]
         (when (not= @@text-ratom @new-text-ratom)
           (reset! text-ratom new-text-ratom)
           (-> @editor-ratom (set-value @new-text-ratom)))

         false)


       :component-did-mount
       (fn [this]
         (let [editor (.edit js/ace (:id opts))
               lang (find-language (:language editor-opts))]
           (.setTheme editor "ace/theme/chrome")
           (-> editor (.setOptions
                        (clj->js {:maxLines                 20
                                  :minLines                 3
                                  :autoScrollEditorIntoView true})))
           (-> editor .getSession (.setMode lang))
           (-> editor .getSession (.setTabSize 2))
           (-> editor .getSession (.on "change" (fn [e] (reset! @text-ratom (.getValue editor)))))
           (-> editor (set-value @@text-ratom))
           (reset! editor-ratom editor)))

       :component-will-unmount
       (fn [this])

       :reagent-render
       (fn [text-ratom-prop]
         (reset! text-ratom text-ratom-prop)
         [:div (dissoc opts :editor)])})))
