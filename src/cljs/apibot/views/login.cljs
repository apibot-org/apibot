(ns apibot.views.login
  (:require
    [apibot.api :as api]
    [apibot.auth0 :as auth0]
    [apibot.raven :as raven]
    [apibot.router :as router]
    [apibot.storage :as storage]
    [apibot.util :as util]
    [apibot.mixpanel :refer [track trackfn]]
    [promesa.core :as p]))

(defn show-error-msg
  ([error]
   (raven/capture-exception error)
   (show-error-msg))
  ([]
   (js/alert "Failed to authenticate. Please check your network connection and try again.")))

(defn- init-bootstrap
  [*app-state]
  (track :ev-login-bootstrap)
  (-> (api/bootstrap! *app-state)
      (p/then (fn [& args]
                (track :ev-login-success)
                (router/goto-editor)))
      (p/catch (fn [error] (show-error-msg error)))))

(defn authenticate [*app-state]
  (auth0/request-auth))


(defn login
  [*app-state]
  [:div {:style {:padding-top "15vh"}}
   [:div.jumbotron
    {:style {:text-align "center"}}
    [:h1 "Write integration tests that rock!"]
    [:p "Welcome to Apibot, a powerful tool for automating API testing made for people who care"
     " about software quality. Fix your bugs before they hit your customers."]
    [:p
     [:button.btn.btn-success.btn-lg
      {:on-click (trackfn :ev-login-login #(authenticate *app-state))}
      "Login"]
     " or "
     [:button.btn.btn-success.btn-lg
      {:on-click (trackfn :ev-login-signup #(authenticate *app-state))}
      "Sign-up"]]]])


