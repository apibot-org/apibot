(ns ^:figwheel-no-load apibot.app
  (:require [apibot.core :as core]
            [devtools.core :as devtools]
            [cljs.spec.test.alpha :as stest]))

(enable-console-print!)

(devtools/install!)

(stest/instrument)

(core/init!)
