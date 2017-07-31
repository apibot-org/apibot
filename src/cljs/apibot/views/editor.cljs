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
  [*app-state]
  (let [*graphs (cursor *app-state [:graphs])
        *selected-graph (commons/find-selected-graph-ratom *app-state)]

    [:div.row
     ;; The Stencil
     [:div.col-xs-2 {:style {:padding    "2px"
                             :max-height "calc(100vh - 51px)"}}
      [stencil/stencil *app-state]]

     ;; The Paper
     [:div.col-xs-7
      [:div
       [:div.row
        [toolbox/toolbox *app-state]]]
      (if *selected-graph
        [:div.row {:style {:height           "calc(100vh - 34px - 51px)"
                           :padding          "0px"
                           :background-color "#eeeeee"}}
         [paper-view *selected-graph]]
        [:div
         [:h2 "Welcome to Apibot"]
         [:p "Check our getting started tutorial or browser the "
          [:a {:href "http://apibot.co/docs/" :target "_blank"} "documentation."]]
         [:iframe
          {:src             "https://player.vimeo.com/video/225732161"
           :width           "640"
           :style           {:display "block" :margin "0 auto"}
           :height          "480"
           :frameBorder     "0"
           :allowFullScreen true}]])]

     ;; The inspector
     (when *selected-graph
       [:div.col-xs-3
        {:style {:padding-right "20px"
                 :overflow-y    "overlay"
                 :overflow-x    "hidden"
                 :max-height    "calc(100vh - 51px)"}}
        [inspector/inspector *app-state *selected-graph]])]))
