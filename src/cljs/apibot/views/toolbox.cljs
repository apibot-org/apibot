(ns apibot.views.toolbox
  "The toolbox is a set of tools that operate on the current graph,
  the most notable being the 'run' functionality"
  (:require
    [apibot.graphs :as graphs]
    [apibot.mixpanel :refer [trackfn]]
    [apibot.grexec :as grexec]
    [apibot.util :as util :refer [swapr!]]
    [apibot.views.commons :refer [publish glyphicon-run]]
    [promesa.core :as p]
    [reagent.core :refer [atom cursor]]))

(defn toolbox
  [*app-state]
  (let [running (atom false)
        graphs-ratom (cursor *app-state [:graphs])
        executions (cursor *app-state [:executions])]
    (fn []
      (let [selected-graph-id (get-in @*app-state [:ui :selected-graph-id])
            graphs (:graphs @*app-state)
            selected-graph (graphs/find-graph-by-id selected-graph-id graphs)]
        [:div.btn-group
         {:style {:margin "2px"}}
         ; The Execute Graph Button
         [:button.btn.btn-default
          {:on-click
                     (trackfn :ev-toolbox-run-graph
                       (fn [e]
                         (reset! running true)
                         (let [promise (grexec/execute! @graphs-ratom selected-graph)]
                           (util/bind-promise!
                             (cursor executions [(:id selected-graph)])
                             promise)
                           (p/finally promise #(reset! running false)))))

           :disabled (or (nil? selected-graph)
                         (not (graphs/executable? selected-graph))
                         @running)}
          [glyphicon-run]
          (if @running
            " Running..."
            " Run")]

         ; The Fit Graph button
         [:button.btn.btn-default
          {:on-click (trackfn :ev-toolbox-fit-graph #(publish :fit-graph nil))
           :type     "button"
           :disabled (nil? selected-graph)}
          [:span.glyphicon.glyphicon-resize-full {:aria-hidden "true"}] " Fit"]

         ; The New Graph Button
         [:button.btn.btn-primary
          {:on-click (trackfn :ev-toolbox-new-graph
                       #(let [g (graphs/empty-graph)]
                         (swap! graphs-ratom conj g)
                         (swap! *app-state assoc-in [:ui :selected-graph-id] (:id g))))}
          [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}]
          " New Graph"]]))))
