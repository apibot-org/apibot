(ns apibot.views.landing-page
  (:require
    [hiccup.page :refer [html5 include-css]]
    [apibot.views.pages.common :as common :refer [common-head common-navbar]]))


(defn section
 ([title message image] (section title message image nil))
 ([title message image learn-more-link]
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
     (common/btn-signup
       "Sign up free")
     (when learn-more-link
       " or ")
     (when learn-more-link
       [:a.btn.btn-info.btn-lg
        {:role "button"
         :href learn-more-link}
        "learn more"])]]
   [:div.col-md-6
    {:style "text-align: center; "}
    [:img {:src   image
           :style "height: 350px;"}]]]))


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
      (common/btn-signup
        "Sign up free")]

     [:div.container

      (section
        "As easy as drag-n-drop"
        "Apibot's visual test editor makes it easy for both technical and non technical members of your team to write complex integration tests.
        All you need is some basic knowledge of HTTP to get started."
        "/img/landing/as-easy-as-drag-n-drop.png"
        "/easy-as-drag-n-drop")

      (section
        "Detailed Bug Reports"
        "Stop wasting time trying to find out what happened. Apibot's bug reports tell you exactly what happened and which steps were taken to reproduce a bug."
        "/img/landing/easy-debugging.png")

      (section
        "Built for teams"
        "Apibot is built with multi-disciplinary teams in mind. Producing high quality software is hard, Apibot gives everyone in your team the tools to make it happen."
        "/img/landing/built-for-teams.png")]

     common/footer]))

