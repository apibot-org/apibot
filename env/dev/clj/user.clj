(ns user
  (:require [mount.core :as mount]
            [apibot.figwheel :refer [start-fw stop-fw cljs]]
            apibot.core))

(defn start []
  (mount/start-without #'apibot.core/repl-server))

(defn stop []
  (mount/stop-except #'apibot.core/repl-server))

(defn restart []
  (stop)
  (start))


