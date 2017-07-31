(ns apibot.storage
  (:refer-clojure :exclude [atom])
  (:require
    [apibot.graphs :refer [editable? map->CustomGraph]]
    [reagent.core :refer [cursor]]
    [clojure.string :refer [join]]))

(defn clear []
  "Clears the local storage"
  (.clear js/localStorage))

(defn set-item
  [key value]
  "Saves the item with the given key."
  {:pre [(string? key)]}
  (if (nil? value)
    (.removeItem js/localStorage key)
    (let [json-string (-> value clj->js js/JSON.stringify)]
      (.setItem js/localStorage (name key) json-string))))

(defn get-item
  [key not-found]
  (if-let [json (.getItem js/localStorage (name key))]
    (-> (js/JSON.parse json)
        (js->clj :keywordize-keys true))
    not-found))

(def is-first-run?
  (memoize
    (fn []
      (let [first-run (get-item :apibot.first-run true)]
        (when first-run
          (set-item :apibot.first-run false))
        first-run))))