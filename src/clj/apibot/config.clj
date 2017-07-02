(ns apibot.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]))

; env can be used to access $ENV variables and obtain other
; environment specific information.
(defstate env :start (load-config
                       :merge
                       [(args)
                        (source/from-resource "config.edn")
                        ;obtained from java's System/getProperties
                        (source/from-system-props)
                        ; obtained from java's System/getenv
                        (source/from-env)]))

(defmacro defprop
  "Usage:

  (defprop auth0-audience)

  will expand to

  (defn auth0-audience []
    (env :auth0-audience))"
  [symbol]
  `(defn ~symbol [] (env ~(-> symbol name keyword))))


(defprop dev)
(defprop auth0-audience)

(defn dev?
  "Return true iff the current environment is dev"
  [] (true? (dev)))

