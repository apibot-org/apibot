(ns apibot.views.executables
  (:require
    [apibot.exec-history :as exec-history]
    [apibot.graphs :as graphs :refer [label]]
    [apibot.grexec :as grexec]
    [apibot.router :as router]
    [apibot.util :as util]
    [apibot.views.commons :as commons :refer [glyphicon-run input-bindable]]
    [clojure.string :refer [lower-case]]
    [reagent.core :refer [cursor atom]]))

;; ---- Model ----

(defn filter-executables
  [graphs]
  (->> (filter graphs/executable? graphs)
       (sort-by (comp lower-case label))))

(defn clear-non-pending-executions!
  [*executions]
  (swap! *executions
         (fn [executions]
           (util/dissoc-if
             (fn [[k v]] (not= :pending (:state v)))
             executions))))

(defn find-graphs-with-failed-executions
  [*app-state]
  (let [graphs (:graphs @*app-state)]
    (->> (:executions @*app-state)
         (filter (fn [[_ {:keys [state value]}]]
                   (or (and (= state :done) (exec-history/error? value))
                       (= state :error))))
         (map (fn [[graph-id _]]
                (graphs/find-graph-by-id graph-id graphs))))))

(defn run-graphs!
  "Executes the given graphs (if possible), bounds the resulting promises to the
  app-state's executables."
  [*app-state graphs-to-execute]
  (let [executions (:executions @*app-state)
        graphs (:graphs @*app-state)
        promise-map (->>
                      graphs-to-execute
                      (filter (fn [graph]
                                (if-let [bound-promise (get executions (:id graph))]
                                  (not= (:state bound-promise) :pending)
                                  true)))
                      (map (fn [graph]
                             [(:id graph)
                              (grexec/execute! graphs graph)])))]
    (doseq [[graph-id promise] promise-map]
      (util/bind-promise! (cursor *app-state [:executions graph-id]) promise))))

(defn export-results!
  [*executions])

(defn execution-result-view
  [graph-id *bound-promise]
  (when @*bound-promise
    [commons/promise-view *bound-promise
     [:span.glyphicon.glyphicon-refresh.rotating {:aria-hidden true}]
     (fn [execution-history]
       (if (exec-history/error? execution-history)
         [:a.btn.btn-danger.btn-xs
          {:href (str "#executions/" graph-id)}
          [:span.glyphicon.glyphicon-exclamation-sign]
          " Error"]
         [:a.btn.btn-success.btn-xs
          {:href (str "#executions/" graph-id)}
          [:span.glyphicon.glyphicon-ok-circle]
          " Success"]))]))

;; ---- Views ----

(defn executables
  [*app-state]
  (let [graphs-cursors (commons/cursor-vec *app-state [:graphs])
        executable-graphs-cursors (filter (comp graphs/executable? deref) graphs-cursors)
        *executions (cursor *app-state [:executions])]
    [:div.row
     [:div.btn-group
      {:style {:padding "4px"}}
      [:button.btn.btn-primary
       {:type     "button"
        :title    "Executes all graphs, except those that are currently being executed."
        :on-click (fn [e] (run-graphs! *app-state (map deref executable-graphs-cursors)))}
       [glyphicon-run]
       "Run all"]
      [:button.btn.btn-default
       {:type     "button"
        :title    "Executes all graphs which have a failed status."
        :on-click (fn [e] (run-graphs! *app-state (find-graphs-with-failed-executions *app-state)))}
       [glyphicon-run]
       "Retry failed"]
      [:button.btn.btn-default
       {:type     "button"
        :title    "Clear all execution results."
        :on-click (fn [e] (clear-non-pending-executions! *executions))}
       "Clear results"]
      [:button.btn.btn-default
       {:type     "button"
        :style {:display "none"}
        :on-click (fn [e] (export-results! *executions))}
       "Export Results"]]
     [:table.table.table-hover
      [:thead
       [:tr
        [:th "Name"]
        [:th "Status"]]]
      [:tbody
       (doall
         (for [*graph executable-graphs-cursors]
           (let [graph-id (:id @*graph)
                 *bound-promise (cursor *executions [graph-id])]
             [:tr {:key graph-id}
              [:td {:style {:width "40%"}}
               [glyphicon-run]
               (label @*graph)]
              [:td
               [execution-result-view graph-id *bound-promise]]])))]]]))
