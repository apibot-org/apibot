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
      {:on-click (trackfn :ev-login-login #(auth0/request-auth))}
      "Login or Sign-up"]]]])

