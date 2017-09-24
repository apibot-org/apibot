(ns apibot.views.inspector.graph
  "An inspector component for editing properties of the graph"
  (:require
    [apibot.coll :as coll]
    [apibot.graphs :as graphs :refer [label remove-graph]]
    [apibot.grexec :as grexec]
    [apibot.views.commons :as commons]
    [apibot.views.commons :refer [form-group-bindable glyphicon-run]]
    [apibot.views.dialogs :as dialogs]
    [apibot.state :refer [*graphs]]
    [reagent.core :refer [cursor]]
    [apibot.router :as router]))

(defn dialog-show-usages
  [graph usages-graphs]
  (dialogs/message-dialog
    "Cannot Delete Graph"
    [:div
     [:p "Graph " (commons/graph-name graph)
      " cannot be deleted because it is being used by "]
     [:ul
      (doall
        (for [g usages-graphs]
          [:li {:key (:id g)}
           [:a {:role "button" :on-click (fn [e]
                                           (dialogs/hide!)
                                           (router/goto-editor (:id g)))}
            (commons/graph-name g)]]))]
     [:p "Please remove all usages to continue."]]))

(defn dialog-confirm-deletion
  [graph]
  (dialogs/dialog-are-you-sure?
    "Deleting a Graph"
    [:span "Are you sure you want to delete " [:b (label graph)] "? This operation cannot be reverted."]
    (fn []
      (coll/swapr! *graphs remove-graph graph))))

(defn graph
  [*graph]
  [:div
   (let [label (graphs/label @*graph)]
     [:h3.page-header
      {:style {:margin-top "30px"}}
      "Graph "
      (if (empty? label)
        [:i "no name"]
        [:span.text-info label])])
   [:div
    (form-group-bindable
      {:name        "Name"
       :help        "Enter a name for this graph."
       :placeholder (str "Defaults to '" (label @*graph) "'")}
      (cursor *graph [:name]))
    (form-group-bindable
      :textarea
      {:name        "Description"
       :placeholder "Enter a description for this graph. This serves mostly as documentation."
       :spec        ::grexec/description}
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

   ; -- Delete Graph Button --
   [:button.btn.btn-danger
    {:type "button"
     :on-click
           (fn [e]
             (let [graph @*graph
                   graphs @*graphs
                   usages (graphs/find-usages (:id graph) graphs)]
               (dialogs/show!
                 (if (empty? usages)
                   (dialog-confirm-deletion graph)
                   (dialog-show-usages graph usages)))))}
    [:span.glyphicon.glyphicon-trash]
    " Delete Graph"]

   (when (not (graphs/loopless? @*graph))
     [commons/warning-sign
      "Loop found: "
      "Graphs with loops cannot be executed."])
   (when (> (count (graphs/connected-components @*graph)) 1)
     [commons/warning-sign
      "Disconnected nodes found: "
      "Graphs with disconnected subgraphs cannot be executed."])
   (when (= (graphs/count-nodes @*graph) 0)
     [commons/warning-sign
      "Empty graph: "
      "Empty graphs cannot be executed. Make sure your graph has at least one node."])])
