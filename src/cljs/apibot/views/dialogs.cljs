(ns apibot.views.dialogs
  (:require
    [promesa.core :as p]
    [reagent.core :refer [atom]]))

(def *dialog
  "nil indicates that there is no visible dialog in the screen. A non-nil value is a view
  that will be drawn in the screen."
  (atom nil))

(defn show! [view]
  (reset! *dialog view))

(defn hide! []
  (reset! *dialog nil))

(defn dialog-view []
  (when-let [view @*dialog]
    [:div
     view
     [:div.modal-backdrop.fade.in]]))

(defn generic-dialog [title body footer]
  [:div.modal {:role  "dialog"
               :style {:display "block"}}
   [:div.modal-dialog {:role "document"}
    [:div.modal-content

     ; A default header with a close button
     [:div.modal-header
      [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
       [:span {:aria-hidden             "true"
               :dangerouslySetInnerHTML {:__html "&times;"}
               :on-click                (fn [e] (hide!))}]]

      [:h4.modal-title title]]

     [:div.modal-body
      body]

     [:div.modal-footer
      footer]]]])

(defn dialog-are-you-sure? [title message handler]
  [generic-dialog
   title
   [:p message]
   [:div
    [:button.btn.btn-default
     {:type "button" :on-click #(hide!)}
     "Cancel"]
    [:button.btn.btn-primary
     {:type "button" :on-click #(do (hide!) (handler))}
     "Yes, I'm sure"]]])
