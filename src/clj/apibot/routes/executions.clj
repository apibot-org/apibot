(ns apibot.routes.executions
  (:require
    [apibot.db.executions :as db.executions]
    [compojure.api.sweet :refer :all]
    [ring.util.http-response :as response :refer [ok]]
    [apibot.schemas :refer [Execution LightExecution]]
    [schema.core :as s]))

(defapi api-executions
  {:swagger {:ui   "/swagger/executions"
             :spec "/swagger/executions.json"
             :data {:info {:version     "1.0.0"
                           :title       "Users API"
                           :description "API for managing execution results."}}}}

  (context "/api/1/executions" []
    :tags ["Executions"]

    (POST "/" []
      :body-params [execution :- Execution]
      :query-params [user-id :- s/Str]
      :summary "Inserts an execution"
      (ok {:inserted (db.executions/insert user-id execution)}))

    (GET "/" []
      :return {:executions [LightExecution]}
      :query-params [user-id :- s/Str]
      :summary "Obtains all executions belonging to the given user."
      (ok {:executions (db.executions/find-light user-id)}))

    (GET "/:execution-id" []
      :return Execution
      :query-params [user-id :- s/Str]
      :path-params [execution-id :- s/Str]
      :summary "Obtains all executions belonging to the given user."
      (ok (db.executions/find-by-id user-id execution-id)))))


