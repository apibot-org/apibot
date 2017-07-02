(ns apibot.env
  (:require
    [apibot.dev-middleware :refer [wrap-dev]]
    [clojure.tools.logging :as log]
    [apibot.config :as config]
    [selmer.parser :as parser]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (println "audience:" (config/auth0-audience) ", dev:" (config/dev))
     (println "\n-=[apibot started successfully using the development profile]=-"))
   :stop
   (fn []
     (println "\n-=[apibot has shut down successfully]=-"))
   :middleware
   wrap-dev})
