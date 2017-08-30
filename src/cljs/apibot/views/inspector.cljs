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
    [apibot.router :as router]))

(def paper-nested-graph (paper/create-paper-class "selected-nested-graph"))

(defn render-selected-node
  [*app-state *selected-node]
  (when-let [selected-node @*selected-node]
    (let [{:keys [graph-id name type]} selected-node
          ; this is the graph that is being rendered in the inspector becaus the node is selected.
          *graph (commons/find-as-cursor *app-state [:graphs] #(= (:id %) graph-id))
          ; this is the graph that is being rendered in the editor.
          *selected-graph (commons/find-selected-graph-ratom *app-state)]
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
         [:div.alert.alert-warning
          [:b "Note: "]
          "This is a preview of " [:code name]
          "'s contents. Changes on this node might affect other graphs."]
         (render-selected-node *app-state (cursor *graph [:nodes 0]))]

        (> (count (:nodes @*graph)) 1)
        [:div
         [:div.alert.alert-warning
          [:b "Note: "]
          "This is a preview of " [:code name] "'s contents"]
         [:div {:style {:pointer-events "none" :height "50vh"}}
          [paper-nested-graph *graph]]
         [:button.btn.btn-default
          {:on-click #(router/goto-editor graph-id)}
          "Open " name]]

        :else
        [:p "No configuration available for " (:name @*selected-node)]))))

(defn inspector
  [*app-state *selected-graph]
  (let [selected-nodes (->> (cursor-vec *selected-graph [:nodes])
                            (filter (comp :selected deref)))]
    (cond
      ;; Current Graph inspector
      (and *selected-graph (= 0 (count selected-nodes)))
      [inspector-graph/graph *app-state *selected-graph]

      ;; Current Node Inspector
      (and *selected-graph (= 1 (count selected-nodes)))
      (render-selected-node *app-state (first selected-nodes))

      ;; Multiple Selected Nodes Inspector
      (and *selected-graph (< 1 (count selected-nodes)))
      [:p (str (count selected-nodes) " nodes selected, no actions available.")]

      :else
      [:p "No actions available"])))
