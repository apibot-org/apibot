(ns apibot.views.inspector.graph
  "An inspector component for editing properties of the graph"
  (:require
    [apibot.graphs :as graphs :refer [label remove-graph]]
    [apibot.grexec :as grexec]
    [apibot.coll :as coll]
    [apibot.views.commons :refer [form-group-bindable glyphicon-run]]
    [apibot.views.dialogs :as dialogs]
    [reagent.core :refer [cursor]]))

(defn graph
  [*app-state *graph]
  [:div
   [:form
    (form-group-bindable
      {:name        "Name"
       :placeholder (str "Defaults to '" (label @*graph) "'")}
      (cursor *graph [:name]))
    (form-group-bindable
      :textarea
      {:name "Description"
       :spec ::grexec/description}
      (cursor *graph [:desc]))
    [:div.checkbox
     [:label
      [:input
       {:type    "checkbox"
        :checked (:executable @*graph)
        :on-change
                 (fn [e]
                   (swap! *graph update :executable not))}]
      [glyphicon-run]
      " Executable"
      [:p.help-block "Only graphs marked with executable can be run."]]]]

   [:button.btn.btn-danger
    {:type "button"
     :on-click
           (fn [e]
             (dialogs/show!
               (dialogs/dialog-are-you-sure?
                 "Deleting a Graph"
                 [:span "Are you sure you want to delete " [:b (label @*graph)] "? This operation cannot be reverted."]
                 (fn []
                   (let [*graphs (cursor *app-state [:graphs])]
                     (reset! *graphs
                             (remove-graph @*graph @*graphs)))))))}
    "Delete '" (coll/limit-string (label @*graph) 20 "...") "'"]

   (when (not (graphs/loopless? @*graph))
     [:div.alert.alert-warning {:role "alert"}
      [:b "Loop found: "]
      "Graphs with loops cannot be executed."])
   (when (> (count (graphs/connected-components @*graph)) 1)
     [:div.alert.alert-warning {:role "alert"}
      [:b "Disconnected nodes found: "]
      "Graphs with disconnected subgraphs cannot be executed"])
   (when (= (graphs/count-nodes @*graph) 0)
     [:div.alert.alert-warning {:role "alert"}
      [:b "Empty graph: "] "Empty graphs cannot be executed.
     Make sure your graph has at least one node."])])
