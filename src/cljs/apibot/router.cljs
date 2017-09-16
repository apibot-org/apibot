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
    [apibot.api :as api]
    [apibot.auth0 :as auth0]
    [apibot.coll :as coll]
    [apibot.mixpanel :as mixpanel]
    [apibot.raven :as raven]
    [apibot.state :as state :refer [*app-state *execution-history>filter-graph-id *selected-node-ids]]
    [apibot.util :as util]
    [apibot.views.dialogs :as dialogs]
    [clojure.string :refer [starts-with?]]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [promesa.core :as p]
    [reagent.session :as session]
    [secretary.core :as secretary])
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
      (session/put! :page :loading)
      (mixpanel/track :ev-bootstrap-init)
      (-> (api/bootstrap! *app-state)
          (p/then (mixpanel/trackfn :ev-bootstrap-success
                                    #(session/put! :page page-name)))
          (p/catch (mixpanel/trackfn :ev-bootstrap-failed
                                     (fn [error]
                                       (println "Error:" error)
                                       (session/put! :page :login))))))

    :else
    (do
      (session/put! :page :login)
      (-> (auth0/handle-auth)
          (p/then (fn [access-token]
                    (println "Authentication Succeeded! proceeding to " page-name)
                    (mixpanel/track :ev-login-success)
                    (handle-request page-name)))
          (p/catch
            (fn [error]
              (util/error error)
              (dialogs/show!
                (dialogs/message-dialog
                  "Authentication Failed"
                  "Apibot failed to authenticate. Please try again."))
              (raven/capture-exception error)
              (mixpanel/track :ev-login-failed)
              (session/put! :page :login)))))))

;; ---- Routes ----
(secretary/set-config! :prefix "#")

(secretary/defroute
  "/" []
  (mixpanel/track :ev-page-root)
  (handle-request :editor))

(secretary/defroute
  "/editor" []
  (mixpanel/track :ev-page-editor)
  (state/reset-selected-graph-by-id! nil)
  (reset! *selected-node-ids #{})
  (handle-request :editor))

(secretary/defroute
  "/editor/:graph-id" [graph-id]
  (mixpanel/track :ev-page-editor)
  (reset! *selected-node-ids #{})
  (handle-request :editor)
  (state/reset-selected-graph-by-id! graph-id))

(secretary/defroute
  "/executions" [query-params]
  (let [{:keys [graph-id]} query-params]
    (reset! *execution-history>filter-graph-id graph-id))
  (mixpanel/track :ev-page-executions)
  (handle-request :executions))

(secretary/defroute
  "/executions/:execution-id" [execution-id]
  (mixpanel/track :ev-page-execution {:execution-id execution-id})
  (session/put! :execution-id execution-id)
  (handle-request :execution))

(secretary/defroute
  "/executables" []
  (mixpanel/track :ev-page-executables)
  (handle-request :executables))

(secretary/defroute
  "/login" []
  (mixpanel/track :ev-page-login)
  (session/put! :page :login))

(secretary/defroute
  "/projects/:project-id" [project-id]
  (coll/reset-in! *app-state [:selected-project-id] project-id)
  (session/put! :page :project)
  (handle-request :project))


(secretary/defroute
  "*" []
  (mixpanel/track :ev-page-unknown {:hash (-> js/window .-location .-hash)})
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

(defn goto
  ([url]
   (aset (-> js/window .-location) "hash" url))
  ([url query-map]
   (goto (str url (util/to-query-params query-map)))))


(defn current-page? [page]
  (starts-with? (-> js/window .-location .-hash) page))


(defn goto-editor
  ([graph-id]
   (goto (str "#editor/" graph-id)))
  ([]
   (goto "#editor")))


(defn goto-executions
  ([{:keys [graph-id]}]
   (goto "#executions" {:graph-id graph-id}))
  ([]
   (goto "#executions")))


(defn goto-project
  [project-id]
  (goto (str "#projects/" project-id)))


(defn in-projects? []
  (current-page? "#projects"))


(defn goto-login []
  (goto "#login"))
