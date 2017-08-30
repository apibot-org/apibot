(ns apibot.http
  "An HTTP client based on httpurr's xhr client"
  (:require
    [#?(:cljs httpurr.client.xhr :clj httpurr.client.aleph) :as client-impl]
    [apibot.coll :as coll]
    [apibot.json :as json]
    [apibot.specs :as specs]
    [clojure.string :refer [lower-case includes?]]
    [httpurr.client :as http]
    [httpurr.status :as status]
    [promesa.core :as p]))

(defn find-header
  "Returns the value of a header in a request/response map"
  [header-name {:keys [headers]}]
  {:pre [(keyword? header-name)]}
  (->> headers
       (filter (fn [[header-key header-val]]
                 (= (-> header-key name lower-case) (name header-name))))
       (map (fn [[_ header-val]] header-val))
       (first)))

(defn- parse-body-as-json
  [response]
  (update response :body json/from-json))

(defn- with-defaults [request]
  (-> request
      (update :http-method #(or % "GET"))
      (update :headers #(or % {}))
      (update :query-params #(or % {}))))

(defn parse-body-if-possible
  "Takes a response/map and parses the body depending on the content type into a clojure
   data structure."
  [response]
  (if-let [content-type (find-header :content-type response)]
    (cond
      (includes? content-type "application/json")
      (parse-body-as-json response)
      :else response)
    response))

(defn format-json-body-if-possible
  [request]
  (let [body (:body request)]
    (if-let [content-type (find-header :content-type request)]
      (if (and (includes? content-type "application/json")
               (not (string? body)))
        (json/to-json body)
        body)
      body)))

(defn ok? [status-or-request]
  (assert (or (number? status-or-request)
              (:status status-or-request))
          (str "Expected status-or-request to be number of request object but was "
               status-or-request))
  (if (number? status-or-request)
    (and (>= status-or-request 200)
         (<= status-or-request 299))
    (ok? (:status status-or-request))))

(defn proxy-if-needed
  "Takes a url string and a boolean proxy?
  Decides if a proxy should be used to handle the request. This is only necessary in the JS environemnt
  to prevent/surpass CORS restrictions.

  On the JVM there is no need for proxying."
  [url proxy?]
  #?(:cljs
      (let [hostname (-> (js/URL. url) .-hostname)
            localhost? (contains? #{"localhost" "127.0.0.1"} hostname)]
        (cond
          ; Case #1: if trying to connect to localhost, don't proxy.
          localhost?
          url
          ; Case #2: if a proxy has been requested, proxy the URL.
          proxy?
          (str "https://apibot-proxy.herokuapp.com/" url)
          ; Case #3: if no proxy has been requested, then don't proxy.
          :else
          url))
     :clj url))


(defn http-request!
  "
  http-method: either a string or keyword
  url: a string
  headers: a possibly nil map
  query-params: a possibly nil map
  body: a possibly nil string or map
  "
  ([request]
   (http-request! request {:proxy false}))
  ([request {:keys [proxy]}]
   (let [{:keys [http-method url headers query-params body]} (with-defaults request)]
     ;; do some validation, nothing out of the ordinary.
     (cond
       (not (specs/url? url))
       (p/rejected (ex-info "The provided url is not valid" url))
       (nil? http-method)
       (p/rejected (ex-info (str "The provided HTTP method is not valid ('" http-method "')") http-method))
       (not (map? query-params))
       (p/rejected (ex-info "The provided query params are not valid" query-params))
       (not (every? #(-> % name empty? not) (keys headers)))
       (p/rejected (ex-info "The provided headers are not valid" headers))
       :else
       (let [;; And format the method e.g. :get instead of "GET"
             method (-> http-method name lower-case keyword)
             ;; if the body is meant to be json,
             body (format-json-body-if-possible request)
             ;; format headers so that header keys are strings
             headers (coll/map-keys name headers)
             ;; if the proxy is requested, send the request first to the proxy server.
             url (proxy-if-needed url proxy)
             ;; craft the promise
             request-obj {:method       method
                          :query-params query-params
                          :url          url
                          :headers      headers
                          :body         body}

             resp-promise (http/send! client-impl/client request-obj)]

         (-> resp-promise
             ;; ensure the body is parsed from JSON
             (p/then (fn [response]
                       (-> response
                           (parse-body-if-possible)
                           (update :headers coll/keywordize-keys))))))))))
