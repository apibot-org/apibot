(ns apibot.views.navbar
  "The global navbar."
  (:require
    [apibot.graphs :as graphs]
    [apibot.env :as env]
    [reagent.core :refer [cursor]]
    [apibot.router :as router]
    [apibot.views.commons :refer [glyphicon-run]]
    [apibot.auth0 :as auth0]))

(defn active-class-if-page [page]
  (if (router/current-page? page)
    "active" ""))

(defn navbar
  [*app-state]
  (let [*profile (cursor *app-state [:profile])]
    [:nav.navbar.navbar-static-top.navbar-inverse {:style {:margin-bottom "0px"}}
     [:div.container-fluid
      [:ul.nav.navbar-nav.navbar-left
       (when (= :dev env/env)
         [:li
          [:a.navbar-brand
           {:on-click (fn [e])}
           " Apibot DEV"]])

       [:li {:class (active-class-if-page "#editor")}
        [:a
         {:href "#editor"}
         [:span.glyphicon.glyphicon-edit {:aria-hidden "true"}] " Editor"]]

       [:li {:class (active-class-if-page "#executables")}
        [:a
         {:href "#executables"}
         [:span.glyphicon.glyphicon-flash {:aria-hidden "true"}] " Executables"]]

       [:li
        [:a
         {:href "http://apibot.co/docs"
          :target "_blank"}
         [:span.glyphicon.glyphicon-education {:aria-hidden "true"}] " Documentation"]]]

      [:ul.nav.navbar-nav.navbar-right
       [:li
        [:a
         {:role "button"
          :on-click #(auth0/logout)}
         [:span.glyphicon.glyphicon-user {:aria-hidden "true"}] " Logout"]]]]]))


