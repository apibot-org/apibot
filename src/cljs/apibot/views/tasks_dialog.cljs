(ns apibot.views.tasks-dialog
  (:require
    [apibot.exec-history :as exec-history]
    [apibot.graphs :as graphs]
    [apibot.router :as router]
    [apibot.storage :as storage]
    [apibot.util :as util]
    [apibot.views.commons :as commons]
    [apibot.views.executables :as executables]
    [promesa.core :as p]
    [reagent.core :refer [cursor atom]]
    [apibot.coll :as coll]))

(defn create-tasks
  "Creates a set of tasks from the app-state"
  [*app-state]
  (let [graphs (:graphs @*app-state)
        executions-cursor-map (commons/cursor-map *app-state [:executions])]
    (->> executions-cursor-map
         (map (fn [[graph-id *bound-promise]]
                {:graph          (graphs/find-graph-by-id graph-id graphs)
                 :*bound-promise *bound-promise}))
         (filter :graph)
         (sort-by (comp graphs/label :graph)))))

(defn find-active-tasks
  [tasks]
  (filter (fn [task]
            (= (-> task :*bound-promise deref :state) :pending))
          tasks))


(defn tasks-dialog-heading
  [*expanded tasks]
  (let [active-tasks (find-active-tasks tasks)]
    [:div.panel-heading
     [:div.panel-title
      "Active tasks"
      (when-not @*expanded
        [:span.badge
         {:style {:margin-left "5px" :margin-top "-2px"}}
         (when-not (empty? active-tasks)
           [:span.glyphicon.glyphicon-refresh.rotating {:aria-hidden true}])
         " "
         (count tasks)])
      " "
      [:span.glyphicon.pull-right
       {:on-click (fn [e] (swap! *expanded not))
        :style    {:cursor       "pointer"
                   :margin-top   "4px"
                   :margin-left  "50px"
                   :margin-right "-4px"}
        :class    (if @*expanded
                    "glyphicon-chevron-down"
                    "glyphicon-chevron-up")}]]]))

(defn tasks-dialog-footer
  [*expanded *executions]
  [:div.panel-footer
   {:style (if @*expanded
             {:padding "4px 4px"}
             {:display "none"})}
   [:div.btn-group {:role "group"}
    [:button.btn.btn-default.btn-xs
     {:on-click #(executables/clear-non-pending-executions! *executions)}
     [:span.glyphicon.glyphicon-remove-circle]
     " Clear"]]])

(defn tasks-dialog
  [*app-state]
  (let [tasks (create-tasks *app-state)
        *expanded (cursor *app-state [:ui :tasks-dialog-expanded])
        *executions (cursor *app-state [:executions])]
    (when (not (empty? tasks))
      [:div.panel.panel-default
       {:style {:position      "fixed"
                :bottom        "1px"
                :margin-bottom "1px"
                :right         "1px"
                :z-index       20}}
       [tasks-dialog-heading *expanded tasks]
       [:div
        {:style (if @*expanded
                  {:max-height "30vh"
                   :overflow-y "auto"}
                  {:display "none"})}
        [:table.table-condensed
         [:tbody
          (doall
            (for [{:keys [graph *bound-promise]} tasks]
              ;; XXX Aparrently one of these isn't being picked up. I'll leave both
              ;; to be on the safe side.
              ^{:key (:id graph)}
              [:tr {:key (:id graph)}
               [:td
                [:div
                 [:a.btn.btn-link
                  {:href (str "#editor/" (:id graph))}
                  (coll/or-empty? (graphs/label graph) [:i "no name"])]]]
               [:td
                [executables/execution-result-view (:id graph) *bound-promise]]]))]]]
       [tasks-dialog-footer *expanded *executions]])))
