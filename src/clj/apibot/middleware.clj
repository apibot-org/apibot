(ns apibot.middleware
  (:require
    [apibot.auth :as auth]
    [apibot.config :refer [env]]
    [apibot.env :refer [defaults]]
    [apibot.layout :refer [*app-context* error-page]]
    [clojure.tools.logging :as log]
    [clojure.string :refer [starts-with?]]
    [immutant.web.middleware :refer [wrap-session]]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.middleware.gzip :refer [wrap-gzip]]
    [ring.middleware.webjars :refer [wrap-webjars]])
  (:import [javax.servlet ServletContext]
           (com.auth0.jwt JWTVerifier)
           (com.auth0.jwt.exceptions JWTVerificationException JWTDecodeException SignatureVerificationException TokenExpiredException InvalidClaimException)
           (com.auth0.jwt.interfaces DecodedJWT Claim)))

(defn wrap-context [handler]
  (fn [request]
    (binding [*app-context*
              (if-let [context (:servlet-context request)]
                ;; If we're not inside a servlet environment
                ;; (for example when using mock requests), then
                ;; .getContextPath might not exist
                (try (.getContextPath ^ServletContext context)
                     (catch IllegalArgumentException _ context))
                ;; if the context is not specified in the request
                ;; we check if one has been specified in the environment
                ;; instead
                (:app-context env))]
      (handler request))))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t)
        (error-page {:status  500
                     :title   "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-logger [handler]
  (fn [request]
    (log/info (str "Request: " request))
    (println "Request (println)" request)
    (-> System/out (.println (str "Request (syso)" request)))
    (let [response (handler request)]
      (log/info (str "Response: " response))
      response)))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params wrap-format)]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-jwt [handler]
  (let [^JWTVerifier verifier (auth/create-verifier)]
    (fn [request]
      ;; XXX protect only the /api/ endpoints. Is this really what we want?
      (if (starts-with? (:uri request) "/api/")
        (let [token (-> request :headers (get "x-apibot-auth"))]
          (try (let [decoded (.verify verifier (or token ""))
                     user-id (-> decoded (.getClaim "http://apibot.co/user_id") .asString)]
                 (if user-id
                   (handler (assoc-in request [:query-params :user-id] user-id))
                   (handler request)))
               (catch JWTDecodeException e
                 (error-page {:status  403
                              :title   "JWT decode exception"
                              :message "Failed to decode the given token"}))
               (catch SignatureVerificationException e
                 (error-page {:status  403
                              :title   "Signature Verification failed"
                              :message "Failed to verify the signature"}))
               (catch TokenExpiredException e
                 (error-page {:status  403
                              :title   "Token Expired"
                              :message "The given token has expired"}))
               (catch InvalidClaimException e
                 (error-page {:status  403
                              :title   "Invalid Claim"
                              :message "Invalid claim"}))
               (catch JWTVerificationException e
                 (error-page {:status  403
                              :title   "JWT Verification Failed"
                              :message "The provided token could not be verified"}))))
        (handler request)))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-jwt
      wrap-formats
      wrap-webjars
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-context
      wrap-internal-error
      wrap-gzip
      wrap-logger))

