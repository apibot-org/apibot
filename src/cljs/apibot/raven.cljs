(ns apibot.raven
  (:require [apibot.env :as env]))

(def raven js/Raven)

(defn init!
  "Initializes the raven SDK"
  []
  (when (= env/env :prod)
    (println "Sentry:" env/sentry-dsn)
    (-> (.config raven env/sentry-dsn)
        (.install))))

(defn capture-exception
  [error]
  (.captureException raven error))
