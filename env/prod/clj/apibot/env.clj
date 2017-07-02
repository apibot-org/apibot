(ns apibot.env
  (:require
    [clojure.tools.logging :as log]
    [apibot.config :as config]))

(def defaults
  {:init
   (fn []
     (println "audience:" (config/auth0-audience) ", dev:" (config/dev))
     (log/info "\n-=[apibot started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[apibot has shut down successfully]=-"))
   :middleware identity})
