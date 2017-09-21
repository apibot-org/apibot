(ns apibot.views.toolbox
  "The toolbox is a set of tools that operate on the current graph,
  the most notable being the 'run' functionality"
  (:require
    [apibot.coll :as coll :refer [swapr!]]
    [apibot.graphs :as graphs]
    [apibot.grexec :as grexec]
    [apibot.mixpanel :refer [trackfn]]
    [apibot.router :as router]
    [apibot.state :refer [*selected-graph *executions *graphs *selected-project]]
    [apibot.util :as util]
    [apibot.views.commons :refer [publish glyphicon-run]]
    [promesa.core :as p]
    [reagent.core :refer [atom cursor]]
    [apibot.views.dialogs :as dialogs]))

(defn button-add-new-graph [*graphs text]
  ; The New Graph Button
  [:button.btn.btn-primary
   {:on-click (trackfn :ev-toolbox-new-graph
                       #(let [g (->> (graphs/empty-graph)
                                     (graphs/with-project (:id @*selected-project)))]
                          (swap! *graphs conj g)
                          (router/goto-editor (:id g))))}
   [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}]
   (str " " text)])

(defn execute-graph [*running *selected-graph]
  (let [selected-graph @*selected-graph
        dag? (graphs/dag? selected-graph)]
    (cond
      (and (not (:executable selected-graph)) dag?)
      (dialogs/show!
        (dialogs/dialog-are-you-sure?
          "Make graph executable?"
          "Are you sure you want to execute this graph? Graphs are not executable by default but by accepting this dialog you will make it executable."
          (fn []
            (coll/swapr! *selected-graph graphs/with-executable true)
            (execute-graph *running *selected-graph))))

      (and (not dag?))
      (dialogs/show!
        (dialogs/message-dialog
          "Unable to Execute"
          "The current graph cannot be executed because it either has loops or disconnected components."))

      :else
      (let [_ (reset! *running true)
            promise (grexec/execute! @*graphs selected-graph)]
        (util/bind-promise!
          (cursor *executions [(:id selected-graph)])
          promise)
        (p/finally promise #(reset! *running false))))))


(defn toolbox
  []
  (let [*running (atom false)]
    (fn []
      (let [selected-graph @*selected-graph]
        [:div.btn-group
         {:style {:margin "2px"}}
         ; The Execute Graph Button
         [:button.btn.btn-default
          {:on-click (trackfn :ev-toolbox-run-graph
                              (fn [e] (execute-graph *running *selected-graph)))


           :disabled (or (nil? selected-graph)
                         @*running)}
          [glyphicon-run]
          (if @*running
            " Running..."
            " Run")]

         ; The Fit Graph button
         [:button.btn.btn-default
          {:on-click (trackfn :ev-toolbox-fit-graph #(publish :fit-graph nil))
           :type     "button"
           :disabled (nil? selected-graph)}
          [:span.glyphicon.glyphicon-resize-full {:aria-hidden "true"}] " Fit"]

         ; The Format Graph button
         ; TODO iron out the bugs and re-enable
         ;[:button.btn.btn-default
         ;   {:on-click (trackfn :ev-toolbox-format-graph #(publish :format-graph nil))
         ;    :type     "button"
         ;    :disabled (or (nil? selected-graph)
         ;                  (not (graphs/loopless? selected-graph)))}
         ;   [:span.glyphicon.glyphicon-sort {:aria-hidden "true"}] " Format"]

         ; The New Graph Button
         [button-add-new-graph *graphs "New Graph"]]))))
