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
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [promesa.core :as p]
    [reagent.core :as reagent :refer [atom cursor]]
    [reagent.session :as session]
    [secretary.core :as secretary :include-macros true])
  (:import goog.History))

;; ---- App State ----

(def *app-state
  (atom
    {:graphs     grexec/graphs
     :executions {}
     :ui         {:selected-graph-id     nil
                  :tasks-dialog-expanded true}}))

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

(defn home-page []
  [:div.container
     [:p "Welcome to the jungle"]])

(def pages
  {:home #'home-page
   :editor #'editor-page
   :executions #'executions-page
   :executables #'executables-page
   :login #'login-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :editor))

(secretary/defroute "/editor" []
  (session/put! :page :editor))

(secretary/defroute "/executions/:graph-id" [graph-id]
  (session/put! :graph-id graph-id)
  (session/put! :page :executions))

(secretary/defroute "/executables" []
  (session/put! :page :executables))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app

(defn mount-components []
  (reagent/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-components))
