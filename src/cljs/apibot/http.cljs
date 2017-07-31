(ns apibot.http
  "An HTTP client based on httpurr's xhr client"
  (:require
    [httpurr.client :as http]
    [httpurr.client.xhr :as xhr]
    [promesa.core :as p]
    [apibot.util :as util]
    [clojure.string :refer [lower-case includes?]]))

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
  (update response :body util/from-json))

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
        (util/to-json body)
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

(defn http-request!
  "
  http-method: either a string or keyword
  url: a string
  headers: a possibly nil map
  query-params: a possibly nil map
  body: a possibly nil string or map
  "
  [request]
  (let [{:keys [http-method url headers query-params body]} (with-defaults request)]
    ;; do some validation, nothing out of the ordinary.
    (cond
      (not (util/url? url))
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
            headers (util/map-keys name headers)
            ;; craft the promise
            resp-promise (http/send! xhr/client
                                     {:method       method
                                      :query-params query-params
                                      :url          url
                                      :headers      headers
                                      :body         body})]
        (-> resp-promise
            ;; ensure the body is parsed from JSON
            (p/then (fn [response]
                      (-> response
                          (parse-body-if-possible)
                          (update :headers util/keywordize-keys)))))))))
