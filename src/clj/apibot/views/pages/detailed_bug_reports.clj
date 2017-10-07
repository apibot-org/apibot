(ns apibot.views.pages.detailed-bug-reports
  (:require
    [hiccup.page :refer [html5 include-css]]
    [apibot.views.pages.common :as common :refer [common-head common-navbar]]))

(defn- section
  [title & messages]
  [:div
   {:style "margin: 80px 0; font-size: 16px;"}
   [:h4 title]
   (into [:p] messages)])

(def detailed-bug-reports
  (html5
    common-head
    [:body
     common-navbar
     [:div.container
      {:style "font-size: 16px"}

      [:h1
       {:style "text-align: center"}
       "Detailed bug reports"]

      [:div
       {:style "max-width: 600px; text-align: center; margin: 40px auto;"}]

      [:p "A testing framework is only as good as its reports which is why we've tried to make "
       "reporting a first class citizen in Apibot. Below you will find some features that make "
       "Apibot's reporting unique"]

      [:div
       {:style "text-align: center; margin: 80px;"}
       [:img
        {:src "/img/landing/assert-status-2xx.png"}]]


      (section
        "Reproducible Tests"
        "Apibot tests are built around the concept of a Scope. The Scope is an immutable
        data structure that represents the state of a test at a given moment in time. Every
        step of a test generates a new scope. <br>
        The advantage of this approach is that
        you always have a complete and reproducible history of the execution of every test. If
        a bug happened you always have the entire execution history to show exactly which steps you
        took to reproduce a bug. Apibot’s scope is effectively equivalent to having a
        debugger running all the time. You can inspect the value of every single value at every
        step of the execution of your program.")

      (section
        "Execution History"
        "Every test that you have ever executed is stored which means that you can always go back "
        "at previous executions and see what happened. This is specially helpful as a way of visualizing"
        "changes to your API over long periods of time.")

      [:div
       {:style "max-width: 800px; text-align: center; margin: 80px auto;"}
       [:h4 "Catch bugs before they hit your customers"]
       [:p "By providing all members of your team with the tools needed to write and understand integration tests, you will get better coverage and catch bugs before they hit your customers."]
       [:p
        (common/btn-signup "Sign Up Free")]]]

     common/footer]))
