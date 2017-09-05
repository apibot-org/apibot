(ns apibot.views.execution
  "A view that displays the result of a single step of execution."
  (:require
    [apibot.exec-history :as exec-history]
    [apibot.graphs :as graphs]
    [apibot.util :as util]
    [apibot.coll :as coll]
    [apibot.router :as router]
    [apibot.views.commons :as commons]
    [apibot.views.tree :as tree]
    [clojure.string :refer [starts-with?]]
    [reagent.core :refer [atom cursor]]
    [apibot.api :as api]
    [promesa.core :as p]))

;; ---- Model ----

(defn exclude-apibot-keys
  [scope]
  (coll/dissoc-if (fn [[k _]] (starts-with? (name k) "apibot|")) scope))

;; ---- Views ----

(defn execution-step-body
  [*selected-step]
  (let [{:keys [node scope]} @*selected-step]
    [:div
     [:h4 "Scope: '" (-> node :name) "'"]
     [tree/tree (exclude-apibot-keys scope)]
     (when-let [request (:apibot|http-request scope)]
       [:div
        [:h4 "Http Request"]
        [tree/tree request]])
     (when-let [response (:apibot|http-response scope)]
       [:div
        [:h4 "Http Response"]
        [tree/tree response]])
     (when-let [message (:apibot|assertion-failed scope)]
       [:div
        [:h4 "Assertion Failed"]
        [tree/tree message]])
     (when-let [error (:apibot|el-error scope)]
       [:div
        [:h4 "Template Error"]
        [tree/tree error]])
     (when-let [error (:apibot|timeout-error scope)]
       [:div
        [:h4 "Timeout Error"]
        [tree/tree error]])
     (when-let [error (:apibot|js-error scope)]
       [:div
        [:h4 "JavaScript Error"]
        [tree/tree error]])
     (when-let [error (:apibot|csv-error scope)]
       [:div
        [:h4 "CSV Error"]
        [tree/tree error]])
     (when-let [error (:apibot|http-error scope)]
       [:div
        [:h4 "Http Error"]
        [tree/tree error]])]))

(defn list-group-item
  [step *selected-step]
  (let [{:keys [node start-time end-time]} step
        millis (- end-time start-time)]
    [:button.list-group-item
     {:type     "button"
      :class    (str (if (= (:id @*selected-step)
                            (:id step))
                       "active"
                       "")
                     (if (:apibot|error (:scope step))
                       " list-group-item-danger"
                       ""))
      :on-click (fn [e] (reset! *selected-step step))}
     [:p (:name node)
      [:span.label.label-info.pull-right (str (int millis) "ms")]]]))

(defn view-load-execution [execution-id *app-state]
  (let [*execution (commons/find-as-cursor *app-state [:execution-history] #(= (:id %) execution-id))
        *executions (cursor *app-state [:execution-history])]
    (p/then (api/find-execution execution-id)
            (fn [result]
              (if *execution
                (reset! *execution result)
                (swap! *executions conj result))))
    [:div
     {:style {:text-align "center"
              :max-width "300px"
              :margin "5% auto"}}
     [:h4 "Loading, please wait"]
     [:div.progress
      [:div.progress-bar.progress-bar-striped.active
       {:style {:width "100%"}}
       [:span.sr-only "loading..."]]]]))

(defn execution
  [execution-id *app-state]
  (let [execution (->> @*app-state
                       :execution-history
                       (filter #(= (:id %) execution-id))
                       (first))
        execution-steps (:history execution)
        *selected-step (atom (first execution-steps))]
    (if (empty? execution-steps)
      [view-load-execution execution-id *app-state]
      [:div
       [:div.page-header {:style {:margin-top "20px"}}
        [:div.btn-group.pull-right
         [:button.btn.btn-default
          {:on-click (fn [] (router/goto-executions))}
          [:span.glyphicon.glyphicon-th-list]
          " Execution History"]
         [:button.btn.btn-primary
          {:on-click #(router/goto-editor (:graph-id execution))}
          [:span.glyphicon.glyphicon-edit]
          " Go to graph"]]
        [:h3 (:name execution) [:small " execution results"]]]
       [:div.row
        [:div.col-md-4
         [:div.list-group
          (-> (for [step execution-steps]
                #^{:key (:id step)}
                [list-group-item step *selected-step])
              (doall))]]
        [:div.col-md-8
         [execution-step-body *selected-step]]]])))
