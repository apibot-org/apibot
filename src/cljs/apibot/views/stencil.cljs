(ns apibot.views.stencil
  (:require
    [apibot.graphs :as graphs :refer [label editable?]]
    [apibot.util :as util :refer [swapr!]]
    [apibot.views.commons :as commons :refer [cursor-vec glyphicon-run]]
    [clojure.string :as s]
    [reagent.core :as reagent :refer [atom cursor]]))

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
    ;; TODO: check if this still works
    (util/swapr! *selected-graph graphs/conj-node node)))

(defn matches-query?
  [query graph]
  (let [name (s/lower-case (or (label graph) ""))
        desc (s/lower-case (or (:desc graph) ""))]
    (or (empty? query)
        (s/includes? name query)
        (s/includes? desc query))))

;; ---- Views ----

(defn tool-view
  [*app-state graph]
  (let [{:keys [id desc executable]} graph
        editable (editable? graph)
        *graphs (cursor *app-state [:graphs])
        *selected-graph-id (cursor *app-state [:ui :selected-graph-id])
        *selected-graph (commons/find-selected-graph-ratom *app-state)
        selected (= @*selected-graph-id (:id graph))]
    [:div.list-group-item
     {:key   id
      :class (if selected "active" "")
      :style {:background-color (when-not (editable? graph) "rgba(170, 220, 255, 0.16)")}}

     [:p.list-group-item-heading
      (when editable
        {:on-click (fn [e] (reset! *selected-graph-id (:id graph)))})
      (when executable [glyphicon-run])
      [:b (if (empty? (label graph)) [:i "no name"] (label graph))]]

     [:div.list-group-item-text
      [:div.btn-group
       [:button.btn.btn-xs.btn-default
        {:on-click
                   (fn [e]
                     (when *selected-graph
                       (add-node-to-graph *selected-graph graph)))
         :disabled (or selected (not *selected-graph))}
        [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}]
        "Add"]

       (when editable
         [:button.btn.btn-xs.btn-default
          {:on-click (fn [e] (reset! *selected-graph-id (:id graph)))}
          [:span.glyphicon.glyphicon-edit {:aria-hidden "true"}]
          "Edit"])

       ; Duplicate the graph.
       (when editable
         [:button.btn.btn-xs.btn-default
          {:on-click (fn [e] (duplicate-graph *graphs graph))}
          [:span.glyphicon.glyphicon-copy {:aria-hidden "true"}]
          "Duplicate"])]]]))

(defn tool-list
  [*app-state *query]
  (let [*graphs (cursor *app-state [:graphs])]
    [:div.list-group
     {:style {:overflow-x "hidden"
              :overflow-y "overlay"
              :margin-bottom "0px"
              :max-height "calc(100vh - 51px - 36px)"}}

     ;; And the list of tools.
     (->> @*graphs
          (sort-by graph-comparator)
          (filter #(not (contains? #{"skippable"} (:id %))))
          (filter #(matches-query? @*query %))
          (map (fn [graph]
                 ^{:key (:id graph)}
                 [tool-view *app-state graph]))
          (doall))]))

(defn stencil
  [*app-state]
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
     [tool-list *app-state *query]]))
