(ns apibot.views.landing-page
  (:require
    [hiccup.page :refer [html5 include-css]]))


(def common-head
  [:head
   (include-css "/css/bootstrap.3.3.7.min.css"
                "/css/main.css")])


(def common-navbar
  [:nav.navbar.navbar-static-top.navbar-inverse
   [:div.container
    [:a.navbar-brand {:href "#"} "Apibot"]
    [:ul.nav.navbar-nav.navbar-left
     [:li [:a {:href "#"} "Documentation"]]
     [:li [:a {:href "#"} "Sign in"]]]]])


(defn section
  [title message image learn-more-link]
  [:div.row
   {:style "display: flex; align-items: center; padding-top: 40px; padding-bottom: 40px;"}
   [:div.col-md-6
    [:div
     [:h2
      {:style "padding-bottom: 20px;"}
      title]
     [:p
      {:style "font-size: 16px; line-height: 24px"}
      message]]
    [:p
     [:br] [:br]
     [:a.btn.btn-success.btn-lg
      {:role "button"}
      "Signup now"]
     " or "
     [:a.btn.btn-info.btn-lg
      {:role "button"
       :href learn-more-link}
      "learn more"]]]
   [:div.col-md-6
    {:style "text-align: center; "}
    [:img {:src   image
           :style "height: 350px;"}]]])


(def page-easy-as-drag-n-drop
  (html5
    common-head
    [:body
     common-navbar]))


(def landing-page
  (html5
    common-head
    [:body
     common-navbar
     [:div.row
      {:style "text-align: center; width: 40%; margin: 0 auto; padding: 40px 0;"}
      [:h1 "API Testing for Teams"]
      [:p
       {:style "font-size: 18px;"}
       "Apibot makes testing your API as easy as drag-n-drop. "]
      [:button.btn.btn-success.btn-lg
       "Sign up free"]]

     [:div.container

      (section
        "As easy as drag-n-drop"
        "Apibot's visual test editor makes it easy for both technical and non technical members of your team to write complex integration tests. "
        "/img/landing/as-easy-as-drag-n-drop.png"
        "/lp/easy-as-drag-n-drop")

      (section
        "Detailed Bug Reports"
        "Stop wasting time trying to find out what happened. Apibot's bug reports tell you exactly what happened and which steps were taken to reproduce a bug."
        "/img/landing/easy-debugging.png"
        "/lp/detailed-bug-reports")

      (section
        "Built for teams"
        "Apibot is built with multi-disciplinary teams in mind. Producing high quality software is hard, Apibot gives everyone in your team the tools to make it happen."
        "/img/landing/built-for-teams.png"
        "/lp/built-for-teams")]

     [:footer
      {:style "background-color: #222; color: white; padding-top: 26px; margin-top: 26px;"}
      [:div.container
       [:div.row
        [:div.col-xs-4
         [:p "Apibot"]
         [:ul
          [:li [:a {:href "/docs"} "Documentation"]]
          [:li [:a {:href "https://medium.com/@The_Real_Apibot"} "Blog"]]
          [:li [:a {:href "https://github.com/apibot-org"} "Github"]]
          [:li [:a {:href "mailto:support@apibot.co"} "Support"]]
          [:li [:a {:href "/terms.html"} "Terms of Service"]]]]]
       [:div.row
        [:p "© 2017 Apibot, All rights reserved"]]]]]))

