(ns apibot.auth0
  "XXX Refactor this, do we even need a namespace, the whole auth flow is super messy..."
  (:require
    [apibot.env :as env]
    [apibot.http :as http :refer [http-request!]]
    [apibot.util :as util]
    [clojure.string :as str :refer [replace starts-with?]]
    [promesa.core :as p]))

(defn fetch-access-token [{:keys [code verifier]}]
  (http-request!
    {:url (str "https://" env/auth0-domain "/oauth/token")
     :http-method :post
     :headers {:content-type "application/json"}
     :body {:grant_type "authorization_code"
            :client_id env/auth0-client-id
            :code_verifier verifier
            :code code
            :redirect_uri env/auth0-redirect-uri}}))

(defn request-auth
  "Returns a promise with the access token if the request succeeded or nil if it didn't"
  ;; TODO implement this!
  [])





