(ns apibot.exec-history)

(defn error?
  "Return true if the given execution history contains at least one node
  with an error."
  [exec-history]
  (->> exec-history
       (map :scope)
       (map :apibot.error)
       (filter true?)
       (first)
       (some?)))

(defn final-scope
  [exec-history]
  (-> (apply max-key :end-time exec-history)
      (get :scope {})))

(defn from-bound-promise
  "Creates an exec-history from a bound-promise. If the bound promise is not done,
  either because it failed or because it is still pending, an empty history is
  returned."
  [bound-promise]
  (if (= (:state bound-promise) :done)
    (:value bound-promise)
    []))
