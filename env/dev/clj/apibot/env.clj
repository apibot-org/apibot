(ns apibot.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [apibot.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[apibot started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[apibot has shut down successfully]=-"))
   :middleware wrap-dev})
