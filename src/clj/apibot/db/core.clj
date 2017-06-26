(ns apibot.db.core
  (:require
    [apibot.config :refer [env]]
    [monger.collection :as mc]
    [monger.core :as mg]
    [monger.operators :refer :all]
    [mount.core :refer [defstate]])
  (:import org.bson.types.ObjectId))

(defstate db*
  :start (-> env :database-url mg/connect-via-uri)
  :stop (-> db* :conn mg/disconnect))

(defstate db
  :start (:db db*))

(defn create-user [user]
  (mc/insert db "users" user))

(defn update-user [id first-name last-name email]
  (mc/update db "users" {:_id id}
             {$set {:first_name first-name
                    :last_name last-name
                    :email email}}))

(defn get-user [id]
  (mc/find-one-as-map db "users" {:_id id}))


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
       (map (fn [graph]
              (mc/find-and-modify db "graphs"
                {:user-id user-id
                 :_id (:_id graph)}
                (assoc graph :user-id user-id)
                {:return-new true
                 :upsert true})))
       (map deserialize-graph)))
