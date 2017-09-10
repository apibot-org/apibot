(ns apibot.util
  (:refer-clojure :exclude [js->clj])
  (:require
    [cljs.pprint :refer [pprint]]
    [cljsjs.papaparse]
    [clojure.string :refer [join split]]
    [promesa.core :as p]
    [apibot.coll :as coll]))

(defn js->clj
  "Equivalent to (js->clj x :keywordize-keys true)"
  [x]
  (cljs.core/js->clj x :keywordize-keys true))

(defn to-query-params
  "Converts a map to a query string. The resulting string always begins with '?'

  Example:

  (to-query-params {:foo 1 :bar true})
  '?foo=1&bar=true'"
  [m]
  (->> m
       (map (fn [[key val]]
             (str (js/encodeURIComponent (name key)) "="
                  (js/encodeURIComponent val))))
       (join "&")
       (str "?")))

(defn puts
  "Prints and returns x. If a message is passed, the message is also printed."
  ([x]
   (pprint x)
   x)
  ([message x]
   (println message)
   (pprint x)))


(defn log [& args]
  (let [log (-> js/console .-log)]
    (apply log args)))


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
                                                    (reject (ex-info (str "Error when loading CSV file '" file "'")
                                                                     (:errors results))))))}))))))




(defn throttle-with-history
  [millis handler]
  (let [*state (atom {:current-value  nil
                      :previous-value nil
                      :last-executed-at 0
                      :timeout-id     nil})]
    (fn [value]
      (coll/reset-in! *state [:current-value] value)
      (when (nil? (:timeout-id @*state))
        (let [timeoutfunc (fn []
                            (let [{:keys [current-value previous-value]} @*state]
                              (handler previous-value current-value)
                              (reset! *state {:current-value  current-value
                                              :previous-value current-value
                                              :last-executed-at (.getTime (new js/Date))
                                              :timeout-id     nil})))
              new-millis (max (- (+ (:last-executed-at @*state) millis)
                                 (.getTime (new js/Date)))
                              0)
              new-timeout-id (js/setTimeout timeoutfunc new-millis)]
          (coll/reset-in! *state [:timeout-id] new-timeout-id))))))

(defn throttle
  "Returns a function with the same signature as func which is guaranteed to be invoked no more than
  once every `millis`. Whenever func is invoked, it will be invoked with the last arguments
  passed-on to func.
  "
  [millis handler]
  (throttle-with-history millis (fn [old new] (handler new))))
