(ns apibot.grexec.http-node
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])
      [apibot.el :as el]
      [apibot.graphs :refer [map->NativeGraph]]
      [apibot.http :refer [http-request!]]
      [apibot.coll :refer [key-vals->map]]
      [cats.monad.either :refer [branch]]
      [clojure.string :refer [lower-case includes?]]
      [promesa.core :as p]
      [clojure.string :as str]))

;; ---- Specs ----

(s/def ::http-method #{"GET" "POST" "PUT" "DELETE" "PATCH" "OPTIONS"})
(s/def ::url (complement empty?))
(s/def ::body string?)
(s/def ::header (fn [{:keys [key val]}]
                  (and (not (empty? key))
                       (not (empty? val)))))
(s/def ::headers (s/* ::header))
(s/def ::props
  (s/keys :req [::http-method
                ::url
                ::body
                ::headers
                ::query-params]))

;; ---- API ----

(defn execute
  "Performs an HTTP request and appends the scope with the
  :apibot|http-request and :apibot|http-response keys."
  [node scope]
  (let [; clear the apibot.http-(request|response) keys from the scope. This way if there is
        ; an error and the request is not executed, it is obvious to the user which request/response
        ; we are talking about.
        scope (dissoc scope :apibot|http-request :apibot|http-response)
        request (-> (:props node)
                    (update :headers key-vals->map)
                    (update :query-params key-vals->map))
        either-request (el/render request scope)]
    (branch either-request
            (fn [error]
              (assoc scope
                :apibot|error true
                :apibot|el-error error))
            (fn [rendered-request]
              (-> (http-request! rendered-request {:proxy true})
                  (p/then
                    (fn [result]
                      ;; XXX apparently Httpur responds with status 0 on IO exceptions.
                      ;; it looks safe to make this check, but this should be revised
                      ;; in the future.
                      (if (= 0 (:status result))
                        (assoc scope
                          :apibot|http-request rendered-request
                          :apibot|error true
                          :apibot|http-error
                          (str "The request could not be executed, likely due to "
                               "connection problems. Please check your internet \n"
                               "connection and try again."))
                        (assoc scope
                          :apibot|http-request rendered-request
                          :apibot|http-response result))))

                  (p/catch
                    (fn [error]
                      (assoc scope
                        :apibot|error true
                        :apibot|http-error error))))))))


(def graph
  (map->NativeGraph
    {:id       "http-request"
     :name     "HTTP Request"
     :desc     "Makes an HTTP Request"
     :execfunc execute
     :spec     nil}))

;; ---- Swagger Import ----

(defn- parse-swagger-url
  [url]
  (-> (str url)
      (str/replace #"\{\w+\}" (fn [m] (str "$" m)))
      (str/replace-first #":" "${root}")))

(defn- parse-swagger-http-method [swagger-http-method]
  (-> swagger-http-method name str/upper-case))

(defn- parse-swagger-http-headers
  [{:keys [consumes]}]
  (if-let [content-type (first consumes)]
    [{:key "content-type" :val content-type}]
    []))

(defn- parse-swagger-query-params
  [parameters]
  (->> parameters
       (filter #(= (:in %) "query"))
       (map (fn [{:keys [name default]}]
              {:key name
               :val (or default "")}))
       (into [])))

(defn parse-swagger
  [swagger-struct]
  (->> (get-in swagger-struct [:paths])
       (map identity)
       (map (fn [[root-url map-verb-to-endpoint]]
              (map (fn [[http-method endpoint]]
                     [root-url http-method endpoint])
                   map-verb-to-endpoint)))
       (apply concat)
       (map (fn [[root-url verb endpoint]]
              (let [{:keys [parameters]} endpoint]
                {:url          (parse-swagger-url root-url)
                 :http-method  (parse-swagger-http-method verb)
                 :query-params (parse-swagger-query-params parameters)
                 :body         ""
                 :headers      (parse-swagger-http-headers endpoint)})))))
