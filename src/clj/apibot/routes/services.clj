(ns apibot.routes.services
  (:require
    [apibot.config :as config]
    [apibot.db.core :as db]
    [clojure.string :refer [split]]
    [clojure.tools.logging :as log]
    [compojure.api.sweet :refer :all]
    [ring.util.http-response :as response :refer :all]
    [apibot.schemas :refer [Graph]]
    [schema.core :as s]))

(defapi api-graphs
  {:swagger {:ui   "/swagger/graphs"
             :spec "/swagger/graphs.json"
             :data {:info {:version     "1.0.0"
                           :title       "Graphs API"
                           :description "An API that deals with graphs"}}}}

  (context "/api/1" []
    :tags ["Root"]

    (DELETE "/purge" []
      ;; TODO find a way of only enabling this endpoint in development
      :summary "purges the database"
      (when config/dev?
        (ok (db/purge-graphs))))

    (context "/graphs" []
      :tags ["Graphs"]

      (GET "/" []
        :return {:graphs [Graph]}
        :query-params [user-id :- s/Str]
        :summary "Returns all the graphs that belong to the current user."
        (ok {:graphs (db/find-graphs-by-user-id user-id)}))

      (DELETE "/" []
        :query-params [user-id :- s/Str ids :- s/Str]
        :summary "Returns all the graphs that belong to the current user."
        (ok {:removed (db/remove-graphs-by-id user-id (split ids #","))}))

      (PUT "/" []
        :return {:graphs [Graph]}
        :body-params [graphs :- [Graph]]
        :summary "Sets a user's graphs"
        :query-params [user-id :- s/Str]
        (ok {:graphs (db/set-graphs-by-user-id user-id graphs)})))))
