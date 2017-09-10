(ns apibot.views.executions
  "A view that displays a list executions"
  (:require
    [apibot.api :as api]
    [apibot.graphs :as graphs]
    [apibot.util :as util]
    [apibot.views.commons :as commons]
    [apibot.views.executables :as executables]
    [apibot.state :refer [*execution-history>filter-graph-id]]
    [promesa.core :as p]
    [reagent.core :refer [atom cursor]]
    [apibot.router :as router]))

;; ---- Model ----

(def *loading-state (atom {:state :pending}))

(defn- fetch-executions
  [*execution-history]
  (let [promise (p/then (api/find-executions)
                        (fn [executions]
                            (reset! *execution-history executions)))]
    (util/bind-promise! *loading-state promise)
    promise))

(def throttled-fetch-executions (util/throttle 5000 fetch-executions))

(defn filter-execution
  [execution]
  (if-let [graph-id @*execution-history>filter-graph-id]
    (= (:graph-id execution) graph-id)
    true))

;; ---- Views ----

(defn table-row [execution]
  (let [{:keys [name created-at]} execution]
    [:tr
     [:td name]
     [:td [executables/execution-result-view2 execution]]
     [:td (str (new js/Date created-at))]]))

(defn button-group [*execution-history]
  [:div.btn-group
   {:style {:padding "4px"}}
   [commons/promise-view *loading-state
     [:button.btn.btn-default
      {:disabled true}
      [:span.glyphicon.glyphicon-refresh.rotating]
      " Loading..."]
     [:button.btn.btn-default
      {:on-click #(fetch-executions *execution-history)}
      [:span.glyphicon.glyphicon-refresh]
      " Refresh"]]
   (when (some? @*execution-history>filter-graph-id)
     [:button.btn.btn-default
      {:on-click #(router/goto-executions)}
      [:span.glyphicon.glyphicon-list-alt]
      " Show All"])])

(defn executions
  [*app-state]
  (let [*execution-history (cursor *app-state [:execution-history])]
    (throttled-fetch-executions *execution-history)

    [:div.row
     [button-group *execution-history]
     [:table.table.table-hover
      [:thead
       [:tr
        [:th "Graph Name"]
        [:th "Result"]
        [:th "Created At"]]]
      [:tbody
       (->> @*execution-history
            (filter filter-execution)
            (map (fn [execution]
                  ^{:key (:id execution)} [table-row execution]))
            (sort-by :created-at)
            (doall))]]]))



