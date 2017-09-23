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
    [clojure.string :refer [starts-with? join]]
    [reagent.core :refer [atom cursor]]
    [apibot.api :as api]
    [promesa.core :as p]
    [apibot.nodes :as nodes]))

;; ---- Model ----

(defn exclude-apibot-keys
  [scope]
  (coll/dissoc-if (fn [[k _]] (starts-with? (name k) "apibot|")) scope))

(defn conditional-classes [m]
  (->> (filter (fn [[css-class bool]] bool) m)
       (map (fn [[css-class bool]] css-class))
       (join " ")))

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


(defn execution-step-preview-http-request
  [node scope selected? select!]
  (let [{:keys [http-method url]} (:props node)
        {:keys [status]} (:apibot|http-response scope)]
    [:button.list-group-item
     {:on-click select!
      :class    (conditional-classes
                  {"active" selected?
                   "list-group-item-danger" (contains? scope :apibot|error)})}
     [:span
      [:b http-method " "]
      (nodes/path-preview url) " : "
      [:span.label
       {:class (cond
                 (coll/in-range? status 200 300) "label-success"
                 (coll/in-range? status 300 400) "label-info"
                 (coll/in-range? status 400 500) "label-warning"
                 :else "label-danger")}
       status]]]))

(defn execution-step-preview-assertion [node scope selected? select!]
  (let [failed? (contains? scope :apibot|error)]
    [:button.list-group-item
     {:on-click select!
      :class    (conditional-classes
                  {"active"                  selected?
                   "list-group-item-danger"  failed?
                   "list-group-item-success" (not failed?)})}
     [:span [:b (if failed? "FAILED: " "PASSED: ")]
            (:name node)]]))

(defn execution-step-preview-default
  [node scope selected? select!]
  [:button.list-group-item
   {:on-click select!
    :class    (conditional-classes
                {"active" selected?
                 "list-group-item-danger" (contains? scope :apibot|error)})}
   [:span (:name node)]])

(defn list-group-item
  [step *selected-step]
  (let [{:keys [node scope]} step
        type (:type node)
        selected? (= (:id @*selected-step) (:id step))
        select! #(reset! *selected-step step)]
    (cond
      (#{"assert" "assert-body" "assert-headers" "assert-status"} type)
      [execution-step-preview-assertion node scope selected? select!]

      (= "http-request" type)
      [execution-step-preview-http-request node scope selected? select!]

      :else
      [execution-step-preview-default node scope selected? select!])))

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
              :max-width  "300px"
              :margin     "5% auto"}}
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
        graph-id (:graph-id execution)
        *selected-step (atom (first execution-steps))]
    (if (empty? execution-steps)
      [view-load-execution execution-id *app-state]
      [:div
       [:div.page-header {:style {:margin-top "20px"}}
        [:div.btn-group.pull-right
         [:button.btn.btn-default
          {:on-click #(router/goto-executions {:graph-id graph-id})}
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
          {:style {:font-family "monospace"}}
          (-> (for [step execution-steps]
                #^{:key (:id step)}
                [list-group-item step *selected-step])
              (doall))]]
        [:div.col-md-8
         [execution-step-body *selected-step]]]])))
