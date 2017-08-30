(ns apibot.grexec.executors
  (:require
    [promesa.core :as p]))

(defn on-js-error
  ([node scope]
   (p/promise
     (assoc scope
       :apibot|error true
       :apibot|js-error
       (str "There was an error when trying to evaluate node \"" (:name node)
            "\"'s JavaScript function."))))
  ([node scope error]
   ;; XXX The error is currently unused but it might come in handy one day hmmm.
   (on-js-error node scope)))

(defn wrap-with-try-catch
  [executor on-error]
  (fn [node scope]
    (try
      (executor node scope)
      (catch #?(:cljs :default
                :clj Exception) e
        (println "Error:" e)
        (on-error node scope e)))))
