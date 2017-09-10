(ns apibot.views.navbar
  "The global navbar."
  (:require
    [apibot.auth0 :as auth0]
    [apibot.env :as env]
    [apibot.graphs :as graphs]
    [apibot.router :as router]
    [apibot.state :refer [*selected-graph]]
    [apibot.views.commons :refer [glyphicon-run]]
    [reagent.core :refer [cursor]]))

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
         {:href (str "#editor/" (:id @*selected-graph))}
         [:span.glyphicon.glyphicon-edit] " Editor"]]

       [:li {:class (active-class-if-page "#executables")}
        [:a
         {:href "#executables"}
         [:span.glyphicon.glyphicon-flash] " Executables"]]

       [:li {:class (active-class-if-page "#executions")}
        [:a
         {:href "#executions"}
         [:span.glyphicon.glyphicon-th-list] " Execution History"]]

       [:li
        [:a
         {:href   "http://apibot.co/docs"
          :target "_blank"}
         [:span.glyphicon.glyphicon-education] " Documentation"]]]

      [:ul.nav.navbar-nav.navbar-right
       [:li
        [:a
         {:role     "button"
          :on-click #(auth0/logout)}
         [:span.glyphicon.glyphicon-user] " Logout"]]]]]))


