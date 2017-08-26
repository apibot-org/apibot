(ns apibot.grexec.csv-node
  (:require
    [apibot.graphs :as graphs :refer [map->NativeGraph]]
    [promesa.core :as p]))

(defn execute
  [node scope]
  (let [rows (-> node :props :rows)
        row-index (:apibot.row-index scope)
        row (nth rows row-index {})]
    (p/promise (merge scope row))))

(defn find-execution-count
  [graph]
  (if-let [start-node (graphs/find-start-node graph)]
    (or (->> (graphs/traverse start-node graphs/successors graph)
             (filter #(= (:type %) "csv"))
             (map #(-> % :props :rows count))
             (apply max))
        1)
    1))

(def graph
  (map->NativeGraph
    {:id       "csv"
     :name     "CSV"
     :desc     "Merges rows from a CSV file into the scope."
     :execfunc execute
     :spec     nil}))
