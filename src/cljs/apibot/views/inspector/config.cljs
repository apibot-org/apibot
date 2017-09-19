(ns apibot.views.inspector.config
  ""
  (:require
    [apibot.grexec :as grexec]
    [apibot.grexec.config-node :as config-node]
    [apibot.coll :refer [remove-element-at]]
    [apibot.views.commons :as commons :refer [form-group-bindable input-bindable cursor-vec]]
    [cljs.spec.alpha :as s]
    [reagent.core :refer [cursor]]
    [apibot.coll :as coll]))

;; --- Model ---

(defn add-new-empty-config
  [props]
  (swap! props conj {:key "" :val ""}))

;; --- Views ---

(defn form-group-prop
  [props-ratom index prop-ratom]
  [:div.form-group
   [:div.row
    ;; The "key" part of the prop
    [:div.col-xs-4 {:style {:padding-right "2px"}}
     [input-bindable
      {:type        "text"
       :class       "form-control"
       :placeholder "key name e.g. 'rootUrl'"}
      (cursor prop-ratom [:key])]]

    ;; The "value" part of the prop
    [:div.col-xs-8 {:style {:padding-left "2px"}}
     [:div.input-group
      [input-bindable
       {:type        "text"
        :class       "form-control"
        :placeholder "key name e.g. 'rootUrl'"}
       (cursor prop-ratom [:val])]
      [:span.input-group-btn
       [:button.btn.btn-default
        {:type     "button"
         :on-click (fn [e] (swap! props-ratom remove-element-at index))}
        "X"]]]]]])

(defn config
  [selected-node]
  (let [props-ratom (cursor selected-node [:props :config])
        prop-forms (->> (cursor-vec selected-node [:props :config])
                        (map-indexed
                          (fn [index prop-ratom]
                            ^{:key index} [form-group-prop
                                           props-ratom
                                           index
                                           prop-ratom])))]
    [:div
     [:div
      [:h4 "Node " [:span.text-info "Config"]]
      [:p.help-block "Appends named values to the scope. Useful for setting variables."]
      ;; The node's name
      [form-group-bindable
       {:name "Name" :spec ::grexec/name}
       (cursor selected-node [:name])]

      ;; Render the list of props
      [:label "Props"]

      prop-forms

      ;; A button that adds a new empty config to the graph.
      [:button.btn.btn-default
       {:type     "button"
        :on-click (fn [e]
                    (swap! props-ratom
                           (fn [props]
                             (conj (or props []) {:key "" :val ""}))))}
       [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}]
       " Add new Property"]]]))
