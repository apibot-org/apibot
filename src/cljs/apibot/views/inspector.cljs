(ns apibot.views.inspector
  (:require
    [apibot.graphs :as graphs :refer [singleton?]]
    [apibot.coll :refer [swapr!]]
    [apibot.views.commons :as commons :refer [cursor-vec]]
    [apibot.views.inspector.assert :as inspector-assert]
    [apibot.views.inspector.assert-body :as inspector-assert-body]
    [apibot.views.inspector.assert-headers :as inspector-assert-headers]
    [apibot.views.inspector.assert-status :as inspector-assert-status]
    [apibot.views.inspector.config :as inspector-config]
    [apibot.views.inspector.evaljs :as inspector-evaljs]
    [apibot.views.inspector.csv :as inspector-csv]
    [apibot.views.inspector.extract-body :as inspector-extract-body]
    [apibot.views.inspector.extract-header :as inspector-extract-header]
    [apibot.views.inspector.graph :as inspector-graph]
    [apibot.views.inspector.http :as inspector-http]
    [apibot.views.paper :as paper]
    [reagent.core :refer [cursor]]
    [apibot.router :as router]
    [apibot.state :refer [*selected-graph *selected-node-ids]]
    [apibot.coll :as coll]))

(def paper-nested-graph (paper/create-paper-class "selected-nested-graph"))

(defn render-selected-node
  [*app-state *selected-node]
  (assert (some? *selected-node) "A nil node cannot be selected")
  (when-let [selected-node @*selected-node]
    (let [{:keys [graph-id name type]} selected-node
          ; this is the graph that is being rendered in the inspector because the node is selected.
          *graph (commons/find-as-cursor *app-state [:graphs] #(= (:id %) graph-id))]
      (cond
        (= type "http-request")
        (inspector-http/http *selected-node)

        (= type "config")
        (inspector-config/config *selected-node)

        (= type "eval")
        [:p "Clojure Eval is no longer supported"]

        (= type "evaljs")
        (inspector-evaljs/evaljs *selected-node)

        (= type "extract-body")
        (inspector-extract-body/extract-body *selected-node)

        (= type "extract-header")
        (inspector-extract-header/extract-header *selected-node)

        (= type "assert")
        (inspector-assert/assert-view *selected-node)

        (= type "assert-status")
        (inspector-assert-status/assert-status *selected-node)

        (= type "assert-body")
        (inspector-assert-body/assert-body *selected-node)

        (= type "assert-headers")
        (inspector-assert-headers/assert-headers *selected-node)

        (= type "csv")
        (inspector-csv/csv *selected-node *selected-graph)

        ;; if the graph is a singleton, render an inspector for the graph's
        ;; first and only node.
        (singleton? @*graph)
        [:div
         (render-selected-node *app-state (cursor *graph [:nodes 0]))
         [commons/warning-sign
          "Note: "
          [:span "This is a preview of " [:code name] "'s contents. Changes on this node might affect other graphs."]]]

        (> (count (:nodes @*graph)) 1)
        [:div
         [:div {:style {:pointer-events "none" :height "50vh"}}
          [paper-nested-graph *graph]]
         [commons/warning-sign
          "Note: "
          [:span "This is a preview of " [:code name] "'s contents"]]
         [:button.btn.btn-default
          {:on-click #(router/goto-editor graph-id)}
          "Open " name]]

        :else
        [:p "No configuration available for " (:name @*selected-node)]))))


(defn top-menu [*selected-graph *selected-node & forms]
  (let [{:keys [id type]} @*selected-node]
    [:div.row
     {:style {:margin-right "-8px"
              :margin-top "2px"}}
     [:div.btn-group.pull-right
      (when (not= "custom" type)
        [:a.btn.btn-default
         {:role "button"
          :target "_blank"
          :href (str "http://apibot.co/docs/graphs/" type)}
         [:span.glyphicon.glyphicon-education]
         " Docs"])
      [:button.btn.btn-default
       {:on-click #(coll/reset-in! *selected-node [:selected] false)}
       [:span.glyphicon.glyphicon-remove-circle]
       " Unselect"]
      [:button.btn.btn-default
       {:on-click #(coll/swapr! *selected-graph graphs/remove-nodes-by-id id)}
       [:span.glyphicon.glyphicon-trash]
       " Delete"]]]))

(defn inspector
  [*app-state]
  (let [selected-nodes (apibot.state/selected-nodes-cursors)]

    (cond
      ;; Current Graph inspector
      (and *selected-graph (= 0 (count selected-nodes)))
      [inspector-graph/graph *selected-graph]

      ;; Current Node Inspector
      (and *selected-graph (= 1 (count selected-nodes)))
      [:div
       [top-menu *selected-graph (first selected-nodes)]
       [render-selected-node *app-state (first selected-nodes)]]

      ;; Multiple Selected Nodes Inspector
      (and *selected-graph (< 1 (count selected-nodes)))
      [:p (str (count selected-nodes) " nodes selected, no actions available.")]

      :else
      [:p "No actions available"])))
