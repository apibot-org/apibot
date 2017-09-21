(ns apibot.views.project
  (:require
    [apibot.state :as state :refer [*selected-project *projects *app-state *graphs]]
    [apibot.views.commons :as commons]
    [reagent.core :refer [cursor atom]]
    [apibot.router :as router]
    [apibot.graphs :as graphs]
    [apibot.coll :as coll]
    [apibot.api :as api]
    [apibot.util :as util]))

; ---- Model ----

(defn default-project? [project]
  (= (:id project) "default"))

(defn filter-in-project [project-id graphs]
  (->> graphs
       (filter #(graphs/in-project? project-id %))
       (filter graphs/editable?)
       (sort-by graphs/label)))

(def throttled-save-project (util/throttle 10000 api/save-project))

; ---- Views ----

(defn list-item-graph [project graph]
  [:div.list-group-item
   {:key (:id graph)}
   (when-not (default-project? project)
     [:div.pull-right
      [:a
       {:role     "button"
        :on-click (fn [e]
                    (let [*graph (state/find-graph-cursor (:id graph))]
                      (coll/swap-in! *graph [:projects] disj (:id project))))}
       [:span.glyphicon.glyphicon-remove-circle]]])
   [:a
    {:role     "button"
     :on-click #(router/goto-editor (:id graph))
     :type     "button"}
    (commons/graph-name graph)]])

(defn buttons [*project]
  (let [project @*project]
    [:div.btn-group.pull-right
     [:button.btn.btn-default
      {:type     "button"
       :disabled (default-project? project)
       :on-click (fn []
                   (router/goto-editor)
                   (api/remove-project (:id project))
                   (swap! *projects dissoc (:id project))
                   (coll/reset-in! *app-state [:selected-project-id] "default"))}
      [:span.glyphicon.glyphicon-trash]
      " Remove"]]))

(defn input-with-suggestions
  [*project *graphs]
  (let [*query (atom "")]
    (fn []
      (let [filtered-graphs (->> @*graphs
                                 (filter #(not (graphs/in-project? (:id @*project) %)))
                                 (filter #(graphs/matches-query? @*query %))
                                 (sort-by graphs/label))]
        [:div.btn-group
         {:style {:width "100%"
                  :padding "4px 0"}}
         ;; -- The Input --
         [:input.form-control
          {:type        "text"
           :value       @*query
           :placeholder "Type a graph's name to assign to project..."
           :on-change   #(reset! *query (-> % .-target .-value))}]

         ;; -- The DropDown --
         (when (and (not (empty? filtered-graphs))
                    (not (empty? @*query)))
           [:ul.dropdown-menu
            {:style {:display "block"}}
            (doall
              (for [graph filtered-graphs]
                [:li {:key (:id graph)}
                 [:a
                  {:on-click (fn [e]
                               (let [*graph (state/find-graph-cursor (:id graph))]
                                 (coll/swap-in! *graph [:projects] conj (:id @*project))))}
                  (commons/graph-name graph)]]))])]))))

(defn project []
  (let [project @*selected-project
        *project-name (cursor *selected-project [:name])
        graphs-in-project (filter-in-project (:id project) @*graphs)]
    [:div
     [:div.page-header {:style {:margin-top "20px"}}
      [buttons *selected-project]
      [:h3
       [:span.text-primary (:name project)]
       [:small " project settings"]]]
     [:div.row
      (when (default-project? project)
        [commons/warning-sign
         "Default Project "
         "This is the default project. It cannot be removed or modified. "
         "All graphs are by default part of this project. You can think of "
         "it as a 'parent project' or 'top-level' project."])

      [:div.col-md-4
       [:div
        [:div.form-group
         [:label.control-label "Project Name"]
         [:input.form-control
          {:type      "text"
           :value     @*project-name
           :on-change (fn [e]
                        (reset! *project-name (-> e .-target .-value))
                        (throttled-save-project @*selected-project))
           :disabled  (default-project? project)}]
         [:p.help-block "Enter a descriptive name for your project"]]

        [:div.form-group
         [:label.control-label "Assigned Graphs"]
         [:p.help-block "This is a list of all graphs assigned to this project. A graph may be assigned to many projects."]

         (when-not (default-project? project)
           [input-with-suggestions *selected-project *graphs])

         [:div.list-group
          (doall
            (for [graph graphs-in-project]
              (list-item-graph project graph)))]
         (when (empty? graphs-in-project)
           [:p "There are 0 graphs in this project."])]]]]]))

