(ns apibot.env
  (:require
    [apibot.dev-middleware :refer [wrap-dev]]
    [clojure.tools.logging :as log]
    [selmer.parser :as parser]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[apibot started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[apibot has shut down successfully]=-"))
   :middleware wrap-dev})
