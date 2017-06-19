(ns ^:figwheel-no-load apibot.app
  (:require [apibot.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
