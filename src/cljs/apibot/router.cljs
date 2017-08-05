(ns apibot.router
  "Implements a simple non-url based router, because the friggin
  accountant/secretary crap doesn't work on electron.

  The model is very simple. Every 'page' is represented as with a simbol
  and a function. The symbol identifies the function. The function can have
  any number of arguments (although most functions will take no arguments).

  To register a page call register! e.g.:
  ```
  (register! :main-page (fn [user-name] [:p \"My name is \" user-name]))
  ```

  To set the current page call dispatch! e.g.:
  ```
  (dispatch! :main-page \"Billy The Kid\")
  ```
  "
  (:require
    [secretary.core :as secretary]
    [clojure.string :refer [starts-with?]]
    [reagent.session :as session]
    [goog.events :as events]
    [apibot.auth0 :as auth0]
    [apibot.views.dialogs :as dialogs]
    [apibot.api :as api]
    [apibot.mixpanel :as mixpanel]
    [apibot.state :as state :refer [*app-state]]
    [goog.history.EventType :as HistoryEventType]
    [promesa.core :as p])
  (:import goog.History))

(declare goto-editor)

(defn reload! []
  (-> js/window .-location .reload))

(defn handle-request
  [page-name]
  (println "Checking if user is authenticated:" (auth0/authenticated?))
  (cond
    (and (auth0/authenticated?) (state/bootstrapped?))
    (session/put! :page page-name)

    (auth0/authenticated?)
    (do
      (-> (auth0/fetch-user-info (api/token!))
          (p/then (fn [{:keys [email sub picture]}]
                    (api/upsert-user {:email email :picture picture :user-id sub})))
          (p/then (fn [{:keys [id email]}]
                    (mixpanel/set-user-id! id)
                    (mixpanel/set! {:$email email}))))
      (session/put! :page :login)
      (-> (api/bootstrap! *app-state)
          (p/then #(session/put! :page page-name))
          (p/catch #(session/put! :page :login))))

    :else
    (do
      (session/put! :page :login)
      (-> (auth0/handle-auth)
          (p/then (fn [access-token]
                    (println "Authentication Succeeded! proceeding to " page-name)
                    (handle-request page-name)))
          (p/catch
            (fn [error]
              (println "Error:" error)
              (session/put! :page :login)))))))

;; ---- Routes ----
(secretary/set-config! :prefix "#")

(secretary/defroute
  "/" []
  (println "Loading root. Authenticated? " (auth0/authenticated?))
  (handle-request :editor))

(secretary/defroute
  "/editor" []
  (handle-request :editor))

(secretary/defroute
  "/executions/:graph-id" [graph-id]
  (session/put! :graph-id graph-id)
  (handle-request :executions))

(secretary/defroute
  "/executables" []
  (handle-request :executables))

(secretary/defroute
  "/login" []
  (session/put! :page :login))

(secretary/defroute
  "*" []
  (println "Handle *")
  (handle-request :editor))

;; ---- History ----

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (println "Navigating to: '" (.-token event) "'")
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; ---- Route Helpers ----

(defn goto [url]
  (aset (-> js/window .-location) "hash" url))

(defn current-page? [page]
  (starts-with? (-> js/window .-location .-hash) page))

(defn goto-editor []
  (goto "#editor"))

(defn goto-login []
  (goto "#login"))
