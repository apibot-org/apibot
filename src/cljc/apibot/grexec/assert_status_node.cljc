(ns apibot.grexec.assert-status-node
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.assertions :refer [expected-http-response-error]]
    [promesa.core :as p]))

;; ---- Spec ----

(s/def ::from integer?)
(s/def ::to integer?)
(s/def ::props
  (s/keys :req [::from ::to]))

;; ---- API ----

(defn execute
  [node scope]
  (let [{:keys [from to template]} (:props node)
        status (-> scope :apibot|http-response :status)]
    (cond
      (not status)
      (p/promise (expected-http-response-error scope))

      (and (>= status from) (<= status to))
      (p/promise scope)

      :else
      (p/promise
        (assoc scope
          :apibot|error true
          :apibot|assertion-failed
          (str "Expected the status to be in the inclusive range of [" from ", " to "]"
               "  but instead found '" status "'"))))))

(def graph
  (map->NativeGraph
    {:id       "assert-status"
     :name     "Assert Status"
     :desc     "Performs an assertion over the status of the last HTTP request"
     :execfunc execute
     :spec     nil}))
