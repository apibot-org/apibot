(ns apibot.views.executions
  "A view that displays a list executions"
  (:require
    [apibot.api :as api]
    [apibot.graphs :as graphs]
    [apibot.util :as util]
    [apibot.views.commons :as commons]
    [apibot.views.executables :as executables]
    [promesa.core :as p]
    [reagent.core :refer [atom cursor]]))

;; ---- Model ----

(def *loading-state (atom {:state :pending}))

(defn- fetch-executions
  [*execution-history]
  (let [promise (p/then (api/find-executions)
                        (fn [executions]
                            (reset! *execution-history executions)))]
    (util/bind-promise! *loading-state promise)
    promise))

(def throttled-fetch-executions (util/throttle 10000 fetch-executions))

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
      "Refresh"]]])

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
       (->> (for [execution @*execution-history]
              ^{:key (:id execution)} [table-row execution])
            (doall))]]]))



