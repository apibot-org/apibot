(ns apibot.core
  (:require
    [apibot.coll :as coll]
    [apibot.grexec :as grexec]
    [apibot.mixpanel :as mixpanel]
    [apibot.raven :as raven]
    [apibot.router :as router]
    [apibot.state :refer [*app-state]]
    [apibot.views.dialogs :as dialogs]
    [apibot.views.editor :as editor]
    [apibot.views.executables :as executables]
    [apibot.views.execution :as execution]
    [apibot.views.executions :as executions]
    [apibot.views.loading :as loading]
    [apibot.views.login :as login]
    [apibot.views.navbar :as navbar]
    [apibot.views.project :as project]
    [apibot.views.tasks-dialog :as tasks-dialog]
    [cljsjs.papaparse]
    [reagent.core :as reagent :refer [atom cursor]]
    [reagent.session :as session]))

;; ---- Views ----

(defn editor-page [& args]
  [:div
   [navbar/navbar *app-state]
   [tasks-dialog/tasks-dialog *app-state]
   [:div.container-fluid
    [editor/editor *app-state]]])

(defn executions-page []
  [:div
   [navbar/navbar *app-state]
   [:div.container-fluid
    [executions/executions *app-state]]])

(defn execution-page []
  [:div
   [navbar/navbar *app-state]
   [:div.container-fluid
    [execution/execution (session/get :execution-id) *app-state]]])

(defn executables-page [& args]
  [:div
   [navbar/navbar *app-state]
   [:div.container-fluid
    [executables/executables *app-state]]])

(defn project-page [& args]
  [:div
   [navbar/navbar *app-state]
   [:div.container-fluid
    [project/project]]])

(defn login-page [& args]
  [:div {:style {:background "url('img/robo-pattern.jpeg')"
                 :height "100vh"}}
   [:div.container
    {:style {:max-width "730px"}}
    [login/login *app-state]]])

(defn loading-page [& args]
  [:div {:style {:background "url('img/robo-pattern.jpeg')"
                 :height "100vh"}}
   [:div.container
    {:style {:max-width "730px"}}
    [loading/loading *app-state]]])

(def pages
  {:editor      #'editor-page
   :executions  #'executions-page
   :execution   #'execution-page
   :executables #'executables-page
   :login       #'login-page
   :project     #'project-page
   :loading     #'loading-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Initialize app

(defn mount-components []
  (reagent/render
    [:div [#'page] [dialogs/dialog-view]]
    (.getElementById js/document "app")))

(defn init! []
  (raven/init!)
  (mixpanel/track :ev-app-load)
  (coll/reset-in! *app-state [:graphs] grexec/graphs)
  (router/hook-browser-navigation!)
  (mount-components))
