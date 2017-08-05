(ns apibot.handler
  (:require
    [apibot.env :refer [defaults]]
    [apibot.layout :refer [error-page]]
    [apibot.middleware :as middleware]
    [apibot.routes.services :refer [api-graphs]]
    [apibot.routes.users :refer [api-users]]
    [compojure.core :refer [routes wrap-routes]]
    [compojure.route :as route]
    [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop ((or (:stop defaults) identity)))

(def app-routes
  (routes
    #'api-users
    #'api-graphs
    (route/not-found
      (:body
        (error-page {:status 404
                     :title  "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
