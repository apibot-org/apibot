(ns apibot.db.core
  (:require
    [apibot.config :refer [env]]
    [monger.collection :as mc]
    [monger.core :as mg]
    [monger.operators :refer [$set $in]]
    [mount.core :refer [defstate]])
  (:import org.bson.types.ObjectId))

(defstate db*
  :start (-> env :database-url mg/connect-via-uri)
  :stop (-> db* :conn mg/disconnect))

(defstate db
  :start (:db db*))

(defn ^ObjectId object-id [^String string]
  (ObjectId. string))

(defn serialize-graph [graph]
  (-> (if-let [^String id (:id graph)]
        (assoc graph :_id (ObjectId. id))
        (assoc graph :_id (ObjectId.)))
      (dissoc :id)))

(defn deserialize-graph [graph]
  (-> (dissoc graph :_id)
      (assoc :id (str (:_id graph)))))

(defn find-graphs-by-user-id [user-id]
  (->> (mc/find-maps db "graphs" {:user-id user-id})
       (map deserialize-graph)))

(defn purge-graphs []
  (mc/purge-many db ["graphs"]))

(defn set-graphs-by-user-id [user-id graphs]
  (->> (map serialize-graph graphs)
       (map #(assoc % :user-id user-id))
       (map (fn [graph]
              (mc/find-and-modify db "graphs"
                                  {:user-id user-id
                                   :_id     (:_id graph)}
                                  graph
                                  {:return-new true
                                   :upsert     true})))
       (map deserialize-graph)))

(defn remove-graphs-by-id [user-id ids]
  (let [obj-ids (->> (map object-id ids)
                     (into []))]
    (mc/remove db "graphs" {:user-id user-id
                            :_id     {$in object-id}})))

