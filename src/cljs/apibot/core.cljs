(ns apibot.core
  (:require
    [apibot.api :as api]
    [apibot.auth0 :as auth0]
    [apibot.grexec :as grexec]
    [apibot.raven :as raven]
    [apibot.storage :as storage]
    [apibot.util :as util]
    [apibot.views.dialogs :as dialogs]
    [apibot.views.editor :as editor]
    [apibot.views.executables :as executables]
    [apibot.views.execution :as execution]
    [apibot.views.login :as login]
    [apibot.views.navbar :as navbar]
    [apibot.views.tasks-dialog :as tasks-dialog]
    [cljsjs.papaparse]
    [apibot.state :refer [*app-state]]
    [promesa.core :as p]
    [reagent.core :as reagent :refer [atom cursor]]
    [reagent.session :as session]
    [secretary.core :as secretary :include-macros true]
    [apibot.router :as router]))

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
    [execution/execution (session/get :graph-id) *app-state]]])

(defn executables-page [& args]
  [:div
   [navbar/navbar *app-state]
   [:div.container-fluid
    [executables/executables *app-state]]])

(defn login-page [& args]
  [:div
   [:div.container
    {:style {:max-width "730px"}}
    [login/login *app-state]]])

(def pages
  {:editor      #'editor-page
   :executions  #'executions-page
   :executables #'executables-page
   :login       #'login-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Initialize app

(defn mount-components []
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (util/reset-in! *app-state [:graphs] grexec/graphs)
  (router/hook-browser-navigation!)
  (mount-components))
