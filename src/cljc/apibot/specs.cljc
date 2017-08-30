(ns apibot.specs
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])))


;; ---- Predicates ----

(defn is-url-valid? [protocol hostname]
  (and (not (empty? hostname))
       (contains? #{"http" "http:" "https" "https:"} protocol)))

(defn url?
  "Returns true iff x is a valid URL"
  [x]
  #?(:cljs
      (try (let [url (new js/URL x)]
             (is-url-valid? (.-protocol url) (.-hostname url)))
           (catch js/Object _ false))
     :clj
      (try
        (let [url (new java.net.URL x)
              protocol (.getProtocol url)
              hostname (.getHost url)]
          (is-url-valid? protocol hostname))
        (catch Exception _ false))))



;; ---- Specs ----

(s/def ::id (s/and string? (complement empty?)))

(s/def ::node any?)
(s/def ::scope (s/and map?))
(s/def ::start-time int?)
(s/def ::end-time int?)

(s/def ::execution-step (s/keys :req-un [::node ::id ::start-time ::end-time ::scope]))
(s/def ::execution (s/coll-of ::execution-step))
