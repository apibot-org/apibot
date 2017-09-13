(ns apibot.db.projects
  (:require
    [apibot.coll :refer [rename-key]]
    [apibot.db.core :as db.core :refer [db]]
    [cats.monad.exception :as exception]
    [monger.collection :as coll])
  (:import (com.mongodb DuplicateKeyException)))

(defn serialize-project [project]
  (rename-key project :id :_id))

(defn deserialize-project [document]
  (-> (rename-key document :_id :id)
      (dissoc :user-id)))

(defn save
  "Saves the given projection"
  [project]
  (let [doc (serialize-project project)]
    (->> (coll/find-and-modify
           db
           "projects"
           {:user-id (:user-id doc) :_id (:_id doc)}
           doc
           {:upsert     true
            :return-new true})
         (deserialize-project)
         (exception/try-on))))

(defn remove-by-id
  "Removes the project with the given ID"
  [user-id project-id]
  (.getN (coll/remove db "projects" {:user-id user-id :_id project-id})))


(defn find-by-user-id
  "Return all projects belonging to the given user"
  [user-id]
  (->> (coll/find-maps db "projects" {:user-id user-id})
       (map deserialize-project)))
