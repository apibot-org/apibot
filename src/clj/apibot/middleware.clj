(ns apibot.middleware
  (:require [apibot.env :refer [defaults]]
            [clojure.tools.logging :as log]
            [apibot.layout :refer [*app-context* error-page]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [muuntaja.middleware :refer [wrap-format wrap-params]]
            [apibot.config :refer [env]]
            [ring.middleware.flash :refer [wrap-flash]]
            [immutant.web.middleware :refer [wrap-session]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [apibot.auth :as auth])
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

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params wrap-format)]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-jwt [handler]
  (let [^JWTVerifier verifier (auth/create-verifier)]
    (fn [request]
      (let [token (-> request :headers (get "x-apibot-auth"))]
        (try (let [decoded (.verify verifier (or token ""))
                   user-id (-> decoded (.getClaim "http://apibot.co/user_id") .asString)]
               (println "User Id: " user-id)
               (handler (assoc-in request [:params :user-id] user-id)))
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
                            :message "The provided token could not be verified"})))))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-formats
      wrap-webjars
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-context
      wrap-jwt
      wrap-internal-error))

