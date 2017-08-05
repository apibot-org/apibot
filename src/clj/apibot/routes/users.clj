(ns apibot.routes.users
  (:require
    [apibot.config :as config]
    [apibot.db.core :as db]
    [apibot.db.users :as db.users]
    [clojure.string :refer [split]]
    [clojure.tools.logging :as log]
    [compojure.api.sweet :refer :all]
    [ring.util.http-response :as response :refer :all]
    [apibot.schemas :refer [User HttpRequest Graph HttpResponse]]
    [schema.core :as s]))

(defapi api-users
  {:swagger {:ui   "/swagger/users"
             :spec "/swagger/users.json"
             :data {:info {:version     "1.0.0"
                           :title       "Users API"
                           :description "API for managing users"}}}}

  (context "/api/1" []
    :tags ["Root"]

    (context "/users" []
      :tags ["Users"]

      (PUT "/me" []
        :return User
        :body-params [user :- User]
        :query-params [user-id :- s/Str]
        :summary "Updates the current user"
        (-> (assoc user :user-id user-id)
            (db.users/upsert)
            (ok))))))

