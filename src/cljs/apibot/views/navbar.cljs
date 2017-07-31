(ns apibot.views.navbar
  "The global navbar."
  (:require
    [apibot.graphs :as graphs]
    [apibot.env :as env]
    [reagent.core :refer [cursor]]
    [apibot.router :as router]
    [apibot.views.commons :refer [glyphicon-run]]))

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
           {:style    {:cursor "pointer"}
            :on-click (fn [e])}
           " Apibot DEV"]])

       [:li {:class (active-class-if-page "#editor")}
        [:a
         {:style {:cursor "pointer"}
          :href  "#editor"}
         [:span.glyphicon.glyphicon-edit {:aria-hidden "true"}] " Editor"]]

       [:li {:class (active-class-if-page "#executables")}
        [:a
         {:style {:cursor "pointer"}
          :href  "#executables"}
         [:span.glyphicon.glyphicon-flash {:aria-hidden "true"}] " Executables"]]]

      [:ul.nav.navbar-nav.navbar-right]]]))

