(ns apibot.json
  #?(:clj
     (:require [clojure.data.json :as json])))



(defn to-json
  "Converts a clojure data structure x to a JSON string."
  [x]
  #?(:cljs
     (-> x clj->js js/JSON.stringify)
     :clj
     (json/write-str x)))


(defn from-json
  "Converts a json string to a clojure data structure."
  [json]
  (when json
    #?(:cljs (-> json js/JSON.parse (js->clj :keywordize-keys true))
       :clj  (json/read-str json :key-fn keyword))))


