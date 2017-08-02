(ns apibot.raven
  (:require [apibot.env :as env]))

;; TODO!!!
(def raven nil)

(defn init!
  "Initializes the raven SDK"
  []
  (println "Sentry:" env/sentry-dsn)
  (-> (.config raven env/sentry-dsn)
      (.install)))

(defn capture-exception
  [error]
  (.captureException raven error))
