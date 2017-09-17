(ns apibot.views.executables
  (:require
    [apibot.exec-history :as exec-history]
    [apibot.graphs :as graphs :refer [label]]
    [apibot.grexec :as grexec]
    [apibot.router :as router]
    [apibot.util :as util]
    [apibot.coll :as coll]
    [apibot.state :refer [*graphs *selected-project]]
    [apibot.views.commons :as commons :refer [glyphicon-run input-bindable]]
    [clojure.string :refer [lower-case]]
    [reagent.core :refer [cursor atom]]))

;; ---- Model ----

(defn clear-non-pending-executions!
  [*executions]
  (swap! *executions
         (fn [executions]
           (coll/dissoc-if
             (fn [[k v]] (not= :pending (:state v)))
             executions))))

(defn find-graphs-with-failed-executions
  [*app-state]
  (let [graphs (:graphs @*app-state)]
    (->> (:executions @*app-state)
         (filter (fn [[_ {:keys [state value]}]]
                   (or (and (= state :done) (exec-history/error? (:history value)))
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
  "TODO: implement this button!"
  [*executions])


(defn execution-result-view2
  [execution]
  (if (or (and (contains? execution :result) (-> execution :result :failed))
          (exec-history/error? (:history execution)))
    [:a.btn.btn-danger.btn-xs
     {:href (str "#executions/" (:id execution))}
     [:span.glyphicon.glyphicon-exclamation-sign]
     " Error"]
    [:a.btn.btn-success.btn-xs
     {:href (str "#executions/" (:id execution))}
     [:span.glyphicon.glyphicon-ok-circle]
     " Success"]))

(defn execution-result-view
  "Takes an execution wrapped in a bound-promise adn a graph-id as input an displays
  a small widget showing the result of the execution."
  [graph-id *bound-promise]
  (when @*bound-promise
    [commons/promise-view *bound-promise
     [:span.glyphicon.glyphicon-refresh.rotating {:aria-hidden true}]
     (fn [execution]                                        ;; <= this is an Execution
       [execution-result-view2 execution])]))


;; ---- Views ----

(defn executables
  [*app-state]
  (let [graphs @*graphs
        executable-graphs (->> graphs
                               (filter #(graphs/in-project? (:id @*selected-project) %))
                               (filter graphs/executable?)
                               (sort-by graphs/label))
        *executions (cursor *app-state [:executions])]
    [:div.row
     [:div.btn-group
      {:style {:padding "4px"}}

      ;; -- button run all --
      [:button.btn.btn-primary
       {:type     "button"
        :title    "Executes all graphs, except those that are currently being executed."
        :on-click (fn [e] (run-graphs! *app-state executable-graphs))}
       [glyphicon-run]
       "Run all"]

      ;; -- button run failed --
      [:button.btn.btn-default
       {:type     "button"
        :title    "Executes all graphs which have a failed status."
        :on-click (fn [e] (run-graphs! *app-state (find-graphs-with-failed-executions *app-state)))}
       [glyphicon-run]
       "Retry failed"]

      ;; -- TODO implement --
      [:button.btn.btn-default
       {:type     "button"
        :style    {:display "none"}
        :on-click (fn [e] (export-results! *executions))}
       "Export Results"]

      ;; -- button clear results --
      [:button.btn.btn-default
       {:type     "button"
        :title    "Clear all execution results."
        :on-click (fn [e] (clear-non-pending-executions! *executions))}
       "Clear results"]]

     ;; -- table executable graphs --
     [:table.table.table-hover
      [:thead
       [:tr
        [:th "Name"]
        [:th "Status"]]]
      [:tbody
       (doall
         (for [graph executable-graphs]
           (let [graph-id (:id graph)
                 *bound-promise (cursor *executions [graph-id])]
             [:tr {:key graph-id}
              [:td {:style {:width "40%"}}
               [glyphicon-run]
               [:a {:href (str "#editor/" graph-id)}
                (coll/or-empty? (label graph) [:i "no name"])]]
              [:td
               [execution-result-view graph-id *bound-promise]]])))]]]))
