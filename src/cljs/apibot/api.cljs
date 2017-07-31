(ns apibot.api
  (:require
    [apibot.env :as env :refer [apibot-root]]
    [apibot.graphs :as graphs]
    [apibot.http :refer [http-request!]]
    [apibot.storage :as storage]
    [apibot.util :as util]
    [clojure.string :as s]
    [httpurr.status :as status]
    [promesa.core :as p]
    [reagent.core :refer [cursor]]
    [secretary.core :as secretary]))

(def *token (atom (storage/get-item :access-token "")))

(defn token? []
  "True iff there is a token"
  (not (empty? @*token)))

(defn node->remote-node
  [{:keys [id graph-id name position props type]}]
  (let [{:keys [x y]} position]
    {:id       id
     :graph-id graph-id
     :name     (or name "")
     :position {:x (or x 0)
                :y (or y 0)}
     :props    (or props {})
     :type     type}))

(defn graph->remote-graph
  "Maps a graph to match the remote model"
  [graph]
  (let [{:keys [id desc edges executable nodes name]} graph]
    {:id         id
     :desc       (or desc "")
     :edges      edges
     :executable executable
     :nodes      (map node->remote-node nodes)
     :name       (or name "")}))

(defn api! [request]
  (-> (assoc-in request [:headers :x-apibot-auth] @*token)
      (http-request!)
      (p/then (fn [response]
                (cond
                  (status/success? response)
                  response
                  (#{401 403} (:status response))
                  (do (reset! *token nil)
                      (storage/set-item :access-token nil)
                      (secretary/dispatch! "/login")
                      (throw (ex-info "Authentication Failed" response)))
                  :else
                  (throw (ex-info "HTTP response failed" response)))))))


(defn fetch-graphs
  "Fires an HTTP request to return the current user's list of graphs.
  Returns a promise which resolves to a collection of graphs."
  []
  (p/then
    (api!
      {:http-method :get
       :url         (str apibot-root "/users/me/graphs")})
    (fn [response]
      (->> (:body response)
           (:graphs)
           (map graphs/map->CustomGraph)))))

(defn update-graphs
  "Fires an HTTP request to update the user's graphs.
  Returns a promise.
  Arguments:
  - graphs: a collection of graphs. Non editable graphs will be filtered out.
  "
  [graphs]
  (let [remote-graphs (->> (filter graphs/editable? graphs)
                           (map graph->remote-graph))]
    (api!
      {:http-method :put
       :body        {:graphs remote-graphs}
       :url         (str apibot-root "/users/me/graphs")
       :headers     {:content-type "application/json"}})))


(defn remove-graphs
  "Removes all graphs with the given IDs."
  [ids]
  (if (not (empty? ids))
    (-> (api! {:http-method  :delete
               :query-params {:ids (s/join "," ids)}
               :url          (str apibot-root "/users/me/graphs")})
        (p/then :body))
    (p/promise {:removed 0})))


(defn sync-graphs
  "Updates and deletes graphs"
  [old-graphs new-graphs]
  (let [ids-to-remove (graphs/find-removed old-graphs new-graphs)]
    (p/all [(remove-graphs ids-to-remove)
            (update-graphs new-graphs)])))

(def sync-graphs-throttled
  "A throttled variant of the sync-graphs function."
  (util/throttle 10000 #(sync-graphs %1 %2)))

(defn bootstrap!
  "Bootstraps the app by loaded all graphs owned by the user and registering a watch function
  over the *app-state"
  ; XXX Probably not the best place to put this function...
  [*app-state]
  {:pre [(token?)]}
  (let [*graphs (cursor *app-state [:graphs])]
    (p/then (fetch-graphs)
            (fn [graphs]
              (println (count graphs) "graphs loaded")
              (swap! *graphs
                     (fn [existing-graphs loaded-graphs]
                       (->> (filter #(not (graphs/editable? %)) existing-graphs)
                            (concat loaded-graphs)
                            (into [])))
                     graphs)


              (println "Registering watch function over *app-state")
              ;; Removing any existing watch ensures that we never have more than one watch
              ;; at any time.
              (remove-watch *app-state :storage)
              (add-watch *app-state :storage
                         (fn [key ref old new]
                           (sync-graphs-throttled (:graphs old) (:graphs new))))))))
