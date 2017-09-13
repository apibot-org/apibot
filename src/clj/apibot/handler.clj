(ns apibot.handler
  (:require
    [apibot.env :refer [defaults]]
    [apibot.layout :refer [error-page]]
    [apibot.middleware :as middleware]
    [apibot.routes.executions :refer [api-executions]]
    [apibot.routes.graphs :refer [api-graphs]]
    [apibot.routes.projects :refer [api-projects]]
    [apibot.routes.users :refer [api-users]]
    [compojure.core :refer [routes wrap-routes defroutes GET]]
    [compojure.route :as route]
    [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop ((or (:stop defaults) identity)))

(defroutes home-routes
  (GET "/" []
    (slurp (clojure.java.io/resource "public/index.html"))))

(def app-routes
  (routes
    #'api-executions
    #'api-graphs
    #'api-projects
    #'api-users
    #'home-routes
    (route/not-found
      (:body
        (error-page {:status 404
                     :title  "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
