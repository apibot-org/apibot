(ns apibot.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [apibot.core-test]))

(doo-tests 'apibot.core-test)

