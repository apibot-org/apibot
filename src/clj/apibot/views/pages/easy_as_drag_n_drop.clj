(ns apibot.views.pages.easy-as-drag-n-drop
  (:require
    [hiccup.page :refer [html5 include-css]]
    [apibot.views.pages.common :as common :refer [common-head common-navbar]]))

(defn- section
  [title & messages]
  [:div
   {:style "margin: 80px 0; font-size: 16px;"}
   [:h4 title]
   (into [:p] messages)])

(def easy-as-drag-n-drop
  (html5
    common-head
    [:body
     common-navbar
     [:div.container
      {:style "font-size: 18px"}

      [:h1
       {:style "text-align: center; padding: 20px;"}
       "As easy as drag-n-drop"]

      [:div
       {:style "max-width: 600px; text-align: center; margin: 40px auto;"}


       [:p "Writing tests should be easy. Not just for the experienced hacker or the savvy tester. "
        "Software quality is everyone’s problem so let’s start solving it together: as a team. "
        "Apibot enables teams to communicate effectively by giving them tools to "
        "read and write API tests."]]

      (section
        "Better coverage"
        "Different users have different use cases in mind. More use cases means better coverage."
        "Developers and product owner/managers usually have a more 'feature' centric approach while QAs, testers and SETs are usually thinking of ways to"
        "abuse or break the system. From a quality perspective you want your tests to encompass every possible use case.")

      (section
        "Knowledge sharing"
        "By having tests that are easy to understand you make it possible for everyone to understand and play with your API. ")

      [:div
       {:style "text-align: center;"}
       [:img
        {:src   "/img/landing/assert-status-2xx.png"}]]

      (section
        "Tests as predicates"
        "As software grows it becomes increasingly difficult for all members of your team to understand its capabilities."
        "You often hear questions like " [:i "'Is it possible to to X?'"] " or " [:i "'should the system do Y?'"] "."
        "Easy to understand and execute tests means that your team can have a centralized body of knowledge where predicated"
        "about the system can be encoded for everyone to understand.")

      (section
        "Executable Documentation"
        "There is no better way of on boarding new people to your team than by showing them exactly what your software can do. This becomes even better if they can actually interact with it, tweak it and see how the API responds to different use cases.")

      [:div
       {:style "max-width: 800px; text-align: center; margin: 80px auto;"}
       [:h4 "Catch bugs before they hit your customers"]
       [:p "By providing all members of your team with the tools needed to write and understand integration tests, you will get better coverage and catch bugs before they hit your customers."]
       [:p
        (common/btn-signup "Sign Up Free")]]]

     common/footer]))
