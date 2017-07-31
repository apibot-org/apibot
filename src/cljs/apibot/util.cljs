(ns apibot.util
  (:refer-clojure :exclude [js->clj])
  (:require
    [cljs.pprint :refer [pprint]]
    [cljsjs.papaparse]
    [clojure.string :refer [join split]]
    [promesa.core :as p]))

(defn js->clj
  "Equivalent to (js->clj x :keywordize-keys true)"
  [x]
  (cljs.core/js->clj x :keywordize-keys true))

(defn to-json
  "Converts a clojure data structure x to a JSON string."
  [x]
  (-> x clj->js js/JSON.stringify))

(defn from-json
  "Converts a json string to a clojure data structure."
  [json]
  (when json
    (-> json js/JSON.parse (js->clj))))

(defn map-vals
  "Given a map f as argument, maps all keys in the map to (f %)"
  [m f]
  (reduce-kv #(assoc %1 %2 (f %3)) {} m))

(defn encode-query-params
  "Encodes a map of query params into a a query-param string
  Example:
  (encode-query-params {:foo 1 :bar \"pizza and cheese\"})
  \"foo=1&bar=pizza%20and%20cheese\""
  [params-map]
  (->> (map-vals params-map #(js/encodeURIComponent %))
       (map (fn [[k v]] (str (name k) "=" v)))
       (join "&")))

(defn decode-query-params
  "Serves as the inverse of encode query params with the exception that it takes a URL as argument"
  [url]
  (let [[_ query-string] (split url "?")]
    (if (empty? query-string)
      {}
      (->> (split query-string "&")
           (map #(split % "="))
           (reduce (fn [result [k v]]
                     (assoc result (keyword k) (js/decodeURIComponent v)))
                   {})))))

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


(defn- key-val-headers->map
  "Converts a collection of [{:key x, :val}] pairs into a map where
  the keys correspond to the :key part and values to the :val part.
  Example:
  (key-val-headers->map [{:key :a :val 1} {:key :b :val 2}])
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


(defn deref?
  [x]
  ;(satisfies? #?(:cljs IDeref :clj clojure.lang.IDeref) x))
  (satisfies? IDeref x))


(defn positions
  [pred coll]
  (assert (vector? coll)
          "(positions pref coll) can only be invoked on a vector since it requires indices.")
  "Returns all positions that match the given predicate"
  (keep-indexed (fn [idx x]
                  (when (pred x)
                    idx))
                coll))


(defn is-js-function?
  "Returns true if the given string represents a javascript function."
  [string]
  (try
    (fn? (js/eval string))
    (catch js/Object e
      false)))


(defn evaluate-js-function
  "Given a javascript string representing a function,
  returns a clojurized function."
  [js-code]
  (let [fun (js/eval js-code)]
    (fn [& args]
      (let [js-args (map clj->js args)]
        (-> (apply fun js-args)
            (js->clj))))))


(defn puts
  "Prints and returns x. If a message is passed, the message is also printed."
  ([x]
   (pprint x)
   x)
  ([message x]
   (println message)
   (pprint x)))

(defn error [message]
  (.error js/console message))

(defn bind-promise!
  [*value promise]
  (when (p/pending? promise)
    (reset! *value {:state :pending}))
  (p/then promise #(reset! *value {:state :done :value %}))
  (p/catch promise #(reset! *value {:state :error :value %})))

(defn read-file [file]
  "
  var reader = new FileReader();
  reader.onload = function(e) {
    var contents = e.target.result;
  };
  reader.readAsText(file);
  "
  (p/promise
    (fn [resolve reject]
      (let [reader (new js/FileReader)]
        (set! (.-onload reader)
              (fn [e]
                (resolve (-> e .-target .-result))))
        (.readAsText reader file "UTF-8")))))

(defn read-csv
  [file delimiter]
  (p/then (read-file file)
          (fn [contents]
            (p/promise
              (fn [resolve reject]
                (.parse js/Papa contents
                        #js {"header"         true
                             "dynamicTyping"  true
                             "skipEmptyLines" true
                             "delimiter"      delimiter
                             "complete"       (fn [results]
                                                (let [results (js->clj results)]
                                                  (if (empty? (:errors results))
                                                    (resolve (:data results))
                                                    (reject (ex-info (str "Error when loading CSV file '" path "'")
                                                                     (:errors results))))))}))))))

(defn read-csv-at-line
  "Returns the parsed CSV line with the given index."
  [path delimiter index]
  (p/then (read-csv path delimiter)
          (fn [results]
            (let [moduled-index (mod index (count results))]
              (nth results moduled-index)))))

(defn url?
  "Returns true iff x is a valid URL"
  [x]
  (try (new js/URL x)
       true
       (catch js/Object _ false)))

(defn throttle
  "Returns a function with the same signature as func which is guaranteed to be invoked no more than
  once every `millis`. Whenever func is invoked, it will be invoked with the last arguments
  passed-on to func.
  "
  ([clock millis func]
   (let [*state (atom {; holds the arguments that are passed to func
                       :args                   nil
                       ; the moment in time when func was last invoked (millis)
                       :last-invocation-millis 0
                       ; boolean flag indicating if there is already a js/setTimeout waiting to be
                       ; invoked.
                       :timeout-id             false})]
     (fn f [& arguments]
       (let [{:keys [args last-invocation-millis timeout-id]} @*state
             now (clock)]
         (if (> now (+ last-invocation-millis millis))
           (do
             (apply func (or args arguments))
             (swap! *state assoc
                    :last-invocation-millis now
                    :timeout-id nil
                    :args nil))
           (do
             (swap! *state assoc :args arguments)
             (when-not timeout-id
               (let [id (js/setTimeout #(apply f (:args @*state)) millis)]
                 (swap! *state assoc :timeout-id id)))))))))
  ([millis func]
   (throttle #(js/Date.now) millis func)))
