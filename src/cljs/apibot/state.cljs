(ns apibot.state
  (:require
    [reagent.core :refer [atom]]
    [apibot.util :as util]))

(def *app-state
  (atom
    {:graphs     []
     :executions {}
     :ui         {:selected-graph-id     nil
                  :tasks-dialog-expanded true
                  :bootstrapped false}}))

(defn bootstrapped? []
  (get-in @*app-state [:ui :bootstrapped]))

(defn bootstrapped! []
  (util/reset-in! *app-state [:ui :bootstrapped] true))
