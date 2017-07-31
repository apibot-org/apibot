(ns apibot.views.inspector.csv
  "An inspector component for the csv-node."
  (:require
    [apibot.grexec.csv-node :as csv-node]
    [apibot.util :as util]
    [apibot.views.commons :refer [form-group-bindable]]
    [promesa.core :as p]
    [reagent.core :refer [cursor]]))

; ---- Model ----

(defn csv-rows->table
  [rows]
  (if (empty? rows)
    []
    (let [;; We assume that all rows have the same keys (because its a CSV file).
          ;; Because this is meant to be a small preview, take only the first 3.
          header-names (take 3 (-> rows first keys))]
      [:table.table.table-hover.table-condensed
       {:style {:max-height "20vh" :overlay-y "auto" :overlay-x "auto"}}
       [:thead
        [:tr
         (map-indexed
           (fn [idx header-name]
             [:th {:key idx} header-name])
           header-names)]]
       [:tbody
        (map-indexed
          (fn [idx-i row]
            [:tr {:key idx-i}
             (map-indexed
               (fn [idx-j value]
                 [:td {:key idx-j} value])
               (vals row))])
          rows)]])))

; ---- Views ----

(defn csv
  [*node *graph]
  (let [*rows (cursor *node [:props :rows])
        *delimiter (cursor *node [:props :delimiter])]
    [:form
     (form-group-bindable
       {:name "Name"}
       (cursor *node [:name]))
     (form-group-bindable
       {:name        "Delimiter"
        :placeholder "Defaults to ','"}
       *delimiter)
     [:div.form-group
      [:label {:for "file-path" :class "control-label"} "CSV File"]
      (if-let [rows @*rows]
        [:div
         [csv-rows->table (take 5 rows)]
         [:div.help-block
          "Showing the first 5 out of " (count rows) " rows and the first 3 columns."
          [:a {:role     "button"
               :on-click #(reset! *rows nil)}
           "Click here"]
          " to select another CSV file."]]
        [:input
         {:id        "file-path"
          :class     "form-control"
          :type      "file"
          :on-change (fn [e]
                       (let [path (-> e .-target .-files (aget 0))]
                         (-> (util/read-csv path @*delimiter)
                             (p/then (fn [rows]
                                       (reset! *rows (into [] (take 100 rows))))))))}])]
     [:div.alert.alert-warning {:role "alert"}
      [:b "NOTE: "]
      "Due to performance reasons, only the first 100 lines are read, "
      "the rest are skipped."]]))
