(ns apibot.views.stencil
  (:require
    [apibot.graphs :as graphs :refer [label editable?]]
    [apibot.coll :refer [swapr!]]
    [apibot.views.commons :as commons :refer [glyphicon-run]]
    [apibot.state :refer [*graphs *selected-graph *selected-project]]
    [clojure.string :as s]
    [reagent.core :as reagent :refer [atom cursor]]
    [apibot.router :as router]))

;; ---- Model ----

(defn graph-comparator
  [graph]
  [(graphs/editable? graph)
   (graphs/executable? graph)
   (graphs/label graph)])

(defn duplicate-graph
  [*graphs graph]
  (swap! *graphs conj (graphs/duplicate-graph graph)))

(defn add-node-to-graph
  "The graph-to-add is the nodes parent graph."
  [*selected-graph graph-to-add]
  (let [;; Create an instance of the graph
        node (graphs/graph->node graph-to-add)]
    (swapr! *selected-graph graphs/conj-node node)))

;; ---- Views ----

(defn tool-view
  [graph]
  (let [{:keys [id desc executable]} graph
        editable (editable? graph)
        selected (= (:id @*selected-graph) (:id graph))]
    [:div.list-group-item
     {:key   id
      :class (if selected "active" "")
      :style {:background-color (when-not (editable? graph) "rgba(170, 220, 255, 0.16)")}}

     [:p.list-group-item-heading
      (when editable
        {:on-click (fn [e] (router/goto-editor (:id graph)))})
      (when executable [glyphicon-run])
      [:b (if (empty? (label graph)) [:i "no name"] (label graph))]]

     [:div.list-group-item-text
      [:div.btn-group
       [:button.btn.btn-xs.btn-default
        {:on-click
                   (fn [e]
                     (add-node-to-graph *selected-graph graph))
         :disabled (or selected (not *selected-graph))}
        [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}]
        "Add"]

       (when editable
         [:button.btn.btn-xs.btn-default
          {:on-click (fn [e]
                       (router/goto-editor (:id graph)))}
          [:span.glyphicon.glyphicon-edit {:aria-hidden "true"}]
          "Edit"])

       ; Duplicate the graph.
       (when editable
         [:button.btn.btn-xs.btn-default
          {:on-click (fn [e] (duplicate-graph *graphs graph))}
          [:span.glyphicon.glyphicon-copy {:aria-hidden "true"}]
          "Duplicate"])]]]))

(defn tool-list
  [*query]
  (let []
    [:div.list-group
     {:style {:overflow-x "hidden"
              :overflow-y "overlay"
              :margin-top "2px"
              :margin-bottom "0px"
              :max-height "calc(100vh - 51px - 36px)"}}

     ;; And the list of tools.
     (->> @*graphs
          (sort-by graph-comparator)
          (filter #(graphs/in-project? (:id @*selected-project) %))
          (filter #(not (contains? #{"skippable"} (:id %))))
          (filter #(graphs/matches-query? @*query %))
          (map (fn [graph]
                 ^{:key (:id graph)}
                 [tool-view graph]))
          (doall))]))

(defn stencil
  []
  (let [*query (atom "")]
    [:div
     ;; a search box for filtering tools.
     [:input.form-control
      {:type        "text"
       :placeholder "Search for ..."
       :on-change
                    (fn [e]
                      (reset! *query (-> e .-target .-value s/lower-case)))}]
     ;; Render the list of tools
     [tool-list *query]]))
