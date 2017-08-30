(ns apibot.coll
  (:require
    [clojure.set :as sets]))

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


(defn map-vals
  "Given a map f as argument, maps all keys in the map to (f %)"
  [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))


(defn limit-string
  "Returns the first n chars of string + append if string is larger than n"
  [string n append]
  (if (>= n (count string))
    string
    (str (subs string 0 n) append)))


(defn remove-element-at
  "Removes the element at the given index from a vector."
  [vector idx]
  (vec
    (concat (subvec vector 0 idx)
            (subvec vector (inc idx)))))


(defn dissoc-if
  "dissocs all keys in m that match the given predicate."
  [kv-predicate m]
  (->> (filter kv-predicate m)
       (map first)
       (apply dissoc m)))


(defn swapr!
  "Equivalent to swap! but f's last argument is the current value.
  Example:
  ```
  (def x (atom [1 2 3]))
  (swapr! x map inc)
  ;; x = [2 3 4]
  ```"
  [atom f & args]
  {:pre [(fn? f)]}
  (swap! atom
         (fn [curr f args]
           (apply f (conj args curr)))
         f (vec args)))


(defn reset-in!
  "Equivalent to reset! but takes a set of keys as the path to where the value will be set.

  Example:
  ```
  (def x (atom {:foo {:bar 1}}))
  (reset-in! x [:foo :bar] 2)
  ;; x == {:foo {:bar 2}}
  ```"
  [*value ks v]
  (swap! *value assoc-in ks v))


(defn swap-in!
  "Equivalent to swap! but takes a set of keys as the path to where the value will be set.

  Example:
  ```
  (def x (atom {:foo {:bar 1}}))
  (swap-in! x [:foo :bar] + 1 1)
  ;; x == {:foo {:bar 3}}
  ```"
  [*value ks f & args]
  (swap! *value
         (fn [value ks f args]
           (apply update-in value ks f args))
         ks f args))


(defn get-nested
  "Similar to (get-in m ks) but takes a vector of functions instead
  of a vector of keys thus being more generic.
  ```"
  ([m fns not-found]
   (loop [current-nested-level m
          remaining-funcs fns]
     (cond
       (nil? current-nested-level)
       not-found

       (empty? remaining-funcs)
       current-nested-level

       :else
       (let [head-func (first remaining-funcs)
             next-nested-level (head-func current-nested-level)]
         (recur next-nested-level (rest remaining-funcs))))))
  ([m fns]
   (get-nested m fns nil)))


(defn key-vals->map
  "Converts a collection of [{:key x, :val}] pairs into a map where
  the keys correspond to the :key part and values to the :val part.
  Example:
  (key-vals->map [{:key :a :val 1} {:key :b :val 2}])
  ;; => {:a 1 :b 2}"
  [key-val-headers]
  (reduce (fn [existing {:keys [key val]}]
            (assoc existing key val))
          {}
          key-val-headers))


(defn map-keys
  "Maps the keys in map m with the given function."
  [f m]
  (reduce (fn [resulting-map key]
            (assoc resulting-map (keyword key) (get m key)))
          {} (keys m)))

(defn keywordize-keys
  "Given a map m as argument, returns a map with the keys converted to
  keywords."
  [m]
  (map-keys #(if (string? %) (keyword %) %) m))


(defn submap
  "Takes a map m as input and a coll of keys and returns a map with only the given keys.

  Example:

  (submap {:foo 1 :bar 2 :baz 3} [:foo :baz])
  => {:foo 1 :baz 3}
  "
  [m ks]
  (keys m)
  (->> (sets/difference (set (keys m)) (set ks))
       (apply dissoc m)))


(defn positions
  [pred coll]
  (assert (vector? coll)
          "(positions pref coll) can only be invoked on a vector since it requires indices.")
  "Returns all positions that match the given predicate"
  (keep-indexed (fn [idx x]
                  (when (pred x)
                    idx))
                coll))


