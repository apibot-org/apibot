(ns apibot.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [args defstate]]))

; env can be used to access $ENV variables and obtain other
; environment specific information.
(defstate env :start (load-config
                       :merge
                       [(args)
                        ;obtained from java's System/getProperties
                        (source/from-system-props)
                        ; obtained from java's System/getenv
                        (source/from-env)]))
