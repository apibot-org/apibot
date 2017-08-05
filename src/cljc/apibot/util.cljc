(ns apibot.util)

(defn rename-key
  "Takes a map as argument and renames the old key for the new.
  If the new key already exists it will be overriden.

  Example:

  (rename-key {:foo 1} :foo :bar)
  (:bar 1)"
  [map key-old key-new]
  (let [val (get map key-old)]
    (-> (assoc map key-new val)
        (dissoc key-old))))

