(ns apibot.views.editor
  "A namespace for the script editor view"
  (:require
    [apibot.util :as util]
    [apibot.graphs :as graphs]
    [apibot.views.commons :as commons :refer [cursor-vec]]
    [apibot.views.inspector :as inspector]
    [apibot.views.paper :as paper]
    [apibot.views.stencil :as stencil]
    [apibot.views.toolbox :as toolbox]
    [reagent.core :as reagent :refer [atom cursor]]))

;; ---- Model ----

(def paper-view (paper/create-paper-class "main-editor"))

;; ---- Views ----

(defn editor
  [selected-graph-id *app-state]
  (let [*selected-graph (commons/find-as-cursor
                          *app-state [:graphs]
                          #(= (:id %) selected-graph-id))]

    [:div.row
     ;; The Stencil
     [:div.col-xs-2 {:style {:padding    "2px"
                             :max-height "calc(100vh - 51px)"}}
      [stencil/stencil *app-state]]

     (when *selected-graph
       ;; The Paper
       [:div.col-xs-6
        [:div
         [:div.row
          [toolbox/toolbox *app-state]]]
        (when *selected-graph
          [:div.row {:style {:height           "calc(100vh - 34px - 51px)"
                             :padding          "0px"
                             :margin-left      "-13px"
                             :background-color "#eeeeee"
                             :border           "1px solid #ddd"
                             :border-radius    "4px"}}
           [paper-view *selected-graph]])])

     ;; The inspector
     (when *selected-graph
       [:div.col-xs-4
        {:style {:padding-right "10px"
                 :overflow-y    "overlay"
                 :overflow-x    "hidden"
                 :max-height    "calc(100vh - 51px)"}}
        [inspector/inspector *app-state *selected-graph]])

     (when-not *selected-graph
       [:div.col-xs-10
        [:div
         [:div.row
          {:style {:text-align "center"}}
          [:h2 "Welcome to Apibot"]
          [:p "Check our getting started tutorial, browser the "
           [:a {:href "http://apibot.co/docs/" :target "_blank"} "documentation "]
           "or"]
          [:p
           [toolbox/button-add-new-graph *app-state "Create a New Graph"]]
          [:iframe
           {:src             "https://player.vimeo.com/video/225732161"
            :width           "480"
            :height          "360"
            :style           {:display "block" :margin "0 auto"}
            :frameBorder     "0"
            :allowFullScreen true}]]]])]))
