(ns apibot.db.executions
  (:require
    [monger.collection :as coll]
    [apibot.coll :refer [rename-key]]
    [monger.query :as query]
    [apibot.exec-history :as exec-history]
    [apibot.db.core :as db.core :refer [db]]
    [clojure.tools.logging :as log])
  (:import (com.mongodb DuplicateKeyException)))

(defn serialize-execution [user-id execution]
  (-> execution
      (rename-key :id :_id)
      (assoc :user-id user-id)))

(defn deserialize-execution [execution]
  (-> execution
      (rename-key :_id :id)))

(defn deserialize-light-execution [execution]
  (let [error (-> execution :history exec-history/error?)]
    (-> execution
        (rename-key :_id :id)
        (dissoc :history)
        (assoc :result {:failed error}))))


(defn insert
  "Inserts the given execution. Returns a boolean value indicating if the operation completed."
  [user-id execution]
  (try
    (do
      (coll/insert
        db "executions"
        (serialize-execution user-id execution))
      true)
    (catch DuplicateKeyException _
      false)))


(defn find-by-id
  "Finds the execution with the given ID."
  [user-id execution-id]
  (-> (coll/find-one-as-map db "executions" {:user-id user-id :_id execution-id})
      deserialize-execution))

(defn find-light
  "Returns the given user's last 100 executions"
  [user-id]
  (->> (query/with-collection
         db "executions"
         (query/find {:user-id user-id})
         (query/fields {:_id 1 :graph-id 1 :name 1 :created-at 1 "history.scope.apibot|error" 1})
         (query/sort (array-map :created-at -1))
         (query/limit 100))
       (map deserialize-light-execution)))
