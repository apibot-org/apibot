(ns apibot.views.loading)

(defn loading
  [*app-state]
  [:div {:style {:padding-top "15vh"}}
   [:div.jumbotron
    {:style {:text-align "center"}}
    [:h1 "Loading..."]
    [:p "Apibot is loading, please wait."]]])


