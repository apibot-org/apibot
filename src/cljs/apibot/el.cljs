(ns apibot.el
  (:require
    [apibot.util :refer [get-nested]]
    [cats.monad.either :refer [right left branch]]
    [clojure.string :refer [ends-with?]]
    [clojure.walk :refer [postwalk]]))

;; ---- Util Functions ----

(defn parse-int [str]
  (js/parseInt str))

(defn get-by-num
  [num]
  (fn [map] (get map num)))

(defn get-by-str-or-kw
  [str-or-kw]
  (fn [map] (or (get map (name str-or-kw))
                (get map (keyword str-or-kw)))))

(defn render-arg-token
  [{:keys [val]} context]
  (->> (clojure.string/split val #"\.")
       (map #(if (re-find #"^\d+$" %)
               (get-by-num (parse-int %))
               (get-by-str-or-kw %)))
       (get-nested context)))

;; ---- Template Functions ----

(defn compile-template
  [string]
  (loop [idx 0
         tokens []]
    (if (>= idx (count string))
      tokens
      (let [chars-left (- (count string) idx)
            chunk (subs string idx (+ idx (min 2 chars-left)))
            chunk (if (ends-with? chunk "$")
                    (subs chunk 0 1)
                    chunk)
            [_ arg] (re-find #"^\$\{(.*?)\}" (subs string idx (count string)))]
        (if arg
          (recur (+ idx (count arg) 3)
                 (conj tokens {:token :arg
                               :idx   idx
                               :val   arg}))
          (recur (+ idx (count chunk))
                 (conj tokens {:token :literal
                               :idx   idx
                               :val   chunk})))))))

(defn render-compiled-template
  [compiled-template context]
  (loop [strings []
         remaining-template compiled-template]
    (if (empty? remaining-template)
      (right (apply str strings))
      (let [head (first remaining-template)
            tail (rest remaining-template)
            {:keys [token val]} head]
        (cond
          ;; Case: literal, leave as is
          (= token :literal)
          (recur (conj strings val) tail)

          ;; Case: arg, fetch from the context
          (= token :arg)
          (if-let [res (render-arg-token head context)]
            (recur (conj strings res) tail)
            (left {:message (str "Unable to find value with key '" val "' in the scope.")})))))))

;; ---- API ----

(defn render-str
  "Renders a template with a context. Returns an Either.
  Use (cats.monad.either/branch result left-fn right-fn) to handle errors."
  [string context]
  (render-compiled-template (compile-template string) context))

(defn render
  [x context]
  (if (string? x)
    (render-str x context)
    (try
      (right
        (postwalk
          (fn [inner]
            (if (string? inner)
              (branch (render-str inner context)
                      #(throw (left %))
                      identity)
              inner))
          x))
      (catch cats.monad.either.Left e
        e))))
