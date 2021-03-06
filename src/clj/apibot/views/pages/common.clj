(ns apibot.views.pages.common
  (:require
    [hiccup.page :refer [html5 include-css include-js]]
    [apibot.config :as config]
    [apibot.coll :as coll]))


(def common-head
  [:head
   [:title "Apibot | write integration tests as easy as drag n drop"]
   (include-css "/css/bootstrap.3.3.7.min.css"
                "/css/main.css")
   (include-js "https://cdn.auth0.com/js/auth0/8.8/auth0.min.js")
   (include-js "/js/auth0.js")
   [:script
    {:async ""
     :src   "https://www.googletagmanager.com/gtag/js?id=UA-100119885-1"}]
   [:script
    "if (/apibot/.test(document.domain)) {
      window.dataLayer = window.dataLayer || [];
      function gtag(){dataLayer.push(arguments);};
      gtag('js', new Date());
      gtag('config', 'UA-100119885-1');
    }"]])

(def common-navbar
  [:nav.navbar.navbar-static-top.navbar-inverse
   [:div.container
    [:a.navbar-brand {:href "/"} "Apibot"]
    [:ul.nav.navbar-nav.navbar-left
     [:li [:a {:href (coll/docs-at)} "Documentation"]]
     [:li [:a {:onclick "apibot_signup()" :role "button"} "Sign in"]]]]])

(defn btn-signup [text]
  [:a.btn.btn-success.btn-lg
   {:onclick "apibot_signup()"}
   text])

(def footer
  "The common footer"
  [:footer
   {:style "background-color: #222; color: white; padding-top: 26px; margin-top: 26px;"}
   [:div.container
    [:div.row
     [:div.col-xs-4
      [:p "Apibot"]
      [:ul
       [:li [:a {:href (coll/docs-at)} "Documentation"]]
       [:li [:a {:href "https://medium.com/@The_Real_Apibot"} "Blog"]]
       [:li [:a {:href "https://github.com/apibot-org"} "Github"]]
       [:li [:a {:href "mailto:support@apibot.co"} "Support"]]
       [:li [:a {:href "/terms.html"} "Terms of Service"]]]]]
    [:div.row
     [:p "© 2017 Apibot, All rights reserved"]]]])

