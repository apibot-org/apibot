(ns apibot.auth0
  "XXX Refactor this, do we even need a namespace, the whole auth flow is super messy..."
  (:require
    [apibot.env :as env]
    [apibot.storage :as storage]
    [promesa.core :as p]
    [apibot.util :as util]))


(def web-auth
  (new js/auth0.WebAuth
       #js {:domain       env/auth0-domain,
            :clientID     env/auth0-client-id,
            :redirectUri  env/auth0-redirect-uri
            :audience     env/auth0-audience,
            :responseType "token id_token",
            :scope        "openid profile email"}))

(defn logout
  "Clears local storage and refreshes the browser thus killing all in-memory state."
  []
  (storage/clear)
  (-> js/window .-location .reload)
  (aset (-> js/window .-location) "href" "#"))

(defn set-session
  [access-token expires-in]
  (let [expires-at (+ (js/Date.now) (* expires-in 1000))]
    (storage/set-item :access-token access-token)
    (storage/set-item :expires-at expires-at)))

(defn authenticated?
  []
  (< (js/Date.now) (storage/get-item :expires-at 0)))

(defn handle-auth []
  (p/promise
    (fn [resolve reject]
      (.parseHash web-auth
                  (fn [err auth-result]
                    (println "Handle Auth:" err auth-result
                             (cond
                               ; Case #1: there is an auth result
                               auth-result
                               (let [access-token (.-accessToken auth-result)
                                     expires-in (.-expiresIn auth-result)]
                                 (set-session access-token expires-in)
                                 (resolve access-token))

                               ; Case #2: There was an error parsing the auth result
                               err
                               (reject (ex-info "Failed to authenticate" err))

                               ; Case #3: There was no auth result, skip silently
                               :else
                               (println "Not handling request since there was no auth-token"))))))))

(defn fetch-user-info [access-token]
  (p/promise
    (fn [resolve reject]
      (.userInfo (.-client web-auth)
                 access-token
                 (fn [err user]
                   (if err
                     (reject (ex-info "Failed to fetch User info" err))
                     (resolve (util/js->clj user))))))))

(defn request-auth []
  (.authorize web-auth))




