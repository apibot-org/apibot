(ns apibot.views.toolbox
  "The toolbox is a set of tools that operate on the current graph,
  the most notable being the 'run' functionality"
  (:require
    [apibot.coll :as coll :refer [swapr!]]
    [apibot.graphs :as graphs]
    [apibot.grexec :as grexec]
    [apibot.mixpanel :refer [trackfn]]
    [apibot.router :as router]
    [apibot.state :refer [*selected-graph *executions *graphs]]
    [apibot.util :as util]
    [apibot.views.commons :refer [publish glyphicon-run]]
    [promesa.core :as p]
    [reagent.core :refer [atom cursor]]))

(defn button-add-new-graph [*graphs text]
  ; The New Graph Button
  [:button.btn.btn-primary
   {:on-click (trackfn :ev-toolbox-new-graph
                       #(let [g (graphs/empty-graph)]
                          (swap! *graphs conj g)
                          (router/goto-editor (:id g))))}
   [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}]
   (str " " text)])

(defn toolbox
  []
  (let [running (atom false)]
    (fn []
      (let [selected-graph @*selected-graph]
        [:div.btn-group
         {:style {:margin "2px"}}
         ; The Execute Graph Button
         [:button.btn.btn-default
          {:on-click
                     (trackfn :ev-toolbox-run-graph
                       (fn [e]
                         (reset! running true)
                         (let [promise (grexec/execute! @*graphs selected-graph)]
                           (util/bind-promise!
                             (cursor *executions [(:id selected-graph)])
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
         [button-add-new-graph *graphs "New Graph"]]))))
