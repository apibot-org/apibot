(ns apibot.routes.services
  (:require
    [apibot.db.core :as db]
    [compojure.api.sweet :refer :all]
    [ring.util.http-response :as response :refer :all]
    [schema.core :as s]
    [clojure.tools.logging :as log]))

(s/defschema User
  {:id (s/maybe s/Str)
   :email s/Str})

(s/defschema Edge
  {:source s/Str
   :target s/Str
   :id s/Str})

(s/defschema Node
  {:id s/Str
   :graph-id s/Str
   :name s/Str
   :position {:x Double :y Double}
   :props s/Any
   :type s/Str})

(s/defschema Graph
  {(s/optional-key :id) (s/maybe s/Str)
   :user-id s/Str
   :desc s/Str
   :edges [Edge]
   :executable s/Str
   :nodes [Node]
   :name s/Str})

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}

  (context "/api/1" []
    :tags ["Root"]

    (DELETE "/purge" []
      :summary     "purges the database"
      (ok (db/purge-graphs)))

    (POST "/users" []
      :return      User
      :body-params [user :- User]
      :summary     "Creates a new user"
      (ok nil))

    (context "/users/me" []
      :tags ["Users"]

      (GET "/graphs" []
        :return      {:graphs [Graph]}
        :summary     "Returns all the graphs that belong to the current user."
        (let [user-id "fernandohur"]
          (ok {:graphs (db/find-graphs-by-user-id user-id)})))

      (PUT "/graphs" []
        :return      {:graphs [Graph]}
        :body-params [graphs :- [Graph]]
        :summary     "Sets a user's graphs"
        (let [user-id "fernandohur"]
          (ok {:graphs (db/set-graphs-by-user-id user-id graphs)}))))))
