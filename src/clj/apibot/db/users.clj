(ns apibot.db.users
  (:require
    [monger.collection :as coll]
    [apibot.db.core :as db.core :refer [db]]))

(defn deserialize-user [user]
  (db.core/normalize-mongo-id user))

(defn upsert
  "Finds and upserts a user. Returns the upserted user."
  [user]
  (-> (coll/find-and-modify
        db "users"
        {:user-id (:user-id user)}
        user
        {:return-new true :upsert true})
      (deserialize-user)))
