(ns apibot.routes.projects
  (:require
    [apibot.db.projects :as db.projects]
    [apibot.schemas :refer [Project]]
    [cats.monad.exception :as exception]
    [compojure.api.sweet :refer [defapi context GET PUT DELETE]]
    [ring.util.http-response :as response :refer [ok]]
    [schema.core :as s]))

(defapi api-projects
  {:swagger {:ui   "/swagger/projects"
             :spec "/swagger/projects.json"
             :data {:info {:version     "1.0.0"
                           :title       "Projects API"
                           :description "API for managing projects"}}}}

  (context "/api/1/projects" []
    :tags ["Projects"]

    (GET "/" []
      :return [Project]
      :query-params [user-id :- s/Str]
      :summary "Returns all the projects that belong to the current user."
      (ok (db.projects/find-by-user-id user-id)))

    (DELETE "/:project-id" []
            :path-params [project-id :- s/Str]
            :query-params [user-id :- s/Str]
            :summary "Returns all the graphs that belong to the current user."
            (ok {:removed (db.projects/remove-by-id user-id project-id)}))

    (PUT "/" []
      :return Project
      :query-params [user-id :- s/Str]
      :body-params [project :- Project]
      :summary "Upserts a project"
      (cond
        (= "default" (:id project))
        (response/bad-request
          {:title   "Default project not allowed"
           :message "The default project cannot be stored."})

        :else
        (-> (db.projects/save (assoc project :user-id user-id))
            (exception/extract)
            (ok))))))

