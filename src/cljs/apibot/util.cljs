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
