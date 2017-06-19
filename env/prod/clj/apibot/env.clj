(ns apibot.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[apibot started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[apibot has shut down successfully]=-"))
   :middleware identity})
