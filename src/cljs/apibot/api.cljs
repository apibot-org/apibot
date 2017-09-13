(ns apibot.api
  (:require
    [apibot.coll :as coll]
    [apibot.env :as env :refer [apibot-root]]
    [apibot.graphs :as graphs]
    [apibot.http :refer [http-request!]]
    [apibot.state :as state]
    [apibot.state :refer [*graphs]]
    [apibot.storage :as storage]
    [apibot.util :as util]
    [clojure.string :as s]
    [httpurr.status :as status]
    [promesa.core :as p]
    [reagent.core :refer [cursor]]
    [secretary.core :as secretary]))

(defn token!
  "Returns the access-token"
  []
  (storage/get-item :access-token nil))

(defn token? []
  "True iff there is a token"
  (not (empty? (token!))))

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
  (let [{:keys [id desc edges executable nodes name projects]} graph]
    {:id         id
     :desc       (or desc "")
     :edges      edges
     :executable executable
     :projects   projects
     :nodes      (map node->remote-node nodes)
     :name       (or name "")}))

(defn execution-step->remote-execution-step
  "Returns the subset of execution which conforms to the ExecutionStep schema"
  [execution-step]
  (-> execution-step
      (coll/submap [:node :id :scope :start-time :end-time])
      (update :node node->remote-node)))

(defn execution->remote-execution
  "Returns the subset of execution which conforms to the Execution schema"
  [execution]
  (-> execution
      (coll/submap [:id :graph-id :name :created-at :history])
      (update :history #(map execution-step->remote-execution-step %))))


(defn api!
  "Makes an HTTP request and appends the x-apibot-auth token.
  If the response is 2xx it is returned.
  Otherwise an exception is thrown.
  "
  [request]
  (-> (assoc-in request [:headers :x-apibot-auth] (token!))
      (http-request!)
      (p/then (fn [response]
                (cond
                  (status/success? response)
                  response
                  (#{401 403} (:status response))
                  (do (storage/set-item :access-token nil)
                      #_(-> js/window .-location .reload)
                      (throw (ex-info "Authentication Failed" response)))
                  :else
                  (throw (ex-info "HTTP response failed" response)))))))

(defn upsert-user
  "Upserts a user."
  [user]
  (-> (api! {:http-method :put
             :headers     {:content-type "application/json"}
             :url         (str apibot-root "/users/me")
             :body        {:user user}})
      (p/then :body)))

(defn fetch-graphs
  "Fires an HTTP request to return the current user's list of graphs.
  Returns a promise which resolves to a collection of graphs."
  []
  (p/then
    (api!
      {:http-method :get
       :url         (str apibot-root "/graphs")})
    (fn [response]
      (->> (:body response)
           (:graphs)
           (map #(update % :projects set))
           (map graphs/create-custom-graph)))))

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
       :url         (str apibot-root "/graphs")
       :headers     {:content-type "application/json"}})))


(defn remove-graphs
  "Removes all graphs with the given IDs."
  [ids]
  (if (not (empty? ids))
    (-> (api! {:http-method  :delete
               :query-params {:ids (s/join "," ids)}
               :url          (str apibot-root "/graphs")})
        (p/then :body))
    (p/promise {:removed 0})))

(defn execute-graph
  [graphs graph-id]
  (assert (some? (graphs/find-graph-by-id graphs graph-id))
          (str "graph-id (" graph-id ") should be present in graphs (" (map :id graphs ")"))))

(defn insert-execution
  [execution]
  (api! {:http-method :post
         :url         (str apibot-root "/executions")
         :headers     {:content-type "application/json"}
         :body        {:execution (execution->remote-execution execution)}}))

(defn find-executions []
  (-> (api! {:http-method :get
             :url         (str apibot-root "/executions")})
      (p/then (comp :executions :body))))

(defn find-execution [execution-id]
  (-> (api! {:http-method :get
             :url         (str apibot-root "/executions/" execution-id)})
      (p/then :body)))

(defn sync-graphs
  "Updates and deletes graphs"
  [old-graphs new-graphs]
  (let [ids-to-remove (graphs/find-removed old-graphs new-graphs)
        graphs-to-update (graphs/find-updated old-graphs new-graphs)]
    (when-not (empty? ids-to-remove)
      (remove-graphs ids-to-remove))
    (when-not (empty? graphs-to-update)
      (update-graphs graphs-to-update))))

(def sync-graphs-throttled
  "A throttled variant of the sync-graphs function."
  (util/throttle-with-history 10000 #(sync-graphs (or %1 []) %2)))

(defn fetch-projects
  []
  "Finds all projects owned by the given user"
  (-> (api! {:http-method :get
             :url (str apibot-root "/projects/")})
      (p/then :body)))


(defn save-project
  "Saves the given project"
  [project]
  (assert (not= (:id project) "default") "Cannot save the default project")
  (-> (api! {:http-method :put
             :url         (str apibot-root "/projects/")
             :headers     {:content-type "application/json"}
             :body        {:project project}})
      (p/then :body)))


(defn remove-project [project-id]
  (-> (api! {:http-method :delete
             :url (str apibot-root "/projects/" project-id)})
      :body))

(defn bootstrap!
  "Bootstraps the app by loading all graphs owned by the user and registering a watch function
  over the *app-state"
  ; XXX Probably not the best place to put this function...
  [*app-state]
  {:pre [(token?)]}
  (let []
    (p/then (p/all [(fetch-graphs) (fetch-projects)])
            (fn [[graphs projects]]
              (println (count graphs) "graphs loaded, " (count projects) " projects loaded")
              (swap! *graphs
                     (fn [existing-graphs loaded-graphs]
                       (->> (filter #(not (graphs/editable? %)) existing-graphs)
                            (concat (if (empty? loaded-graphs)
                                      state/samples
                                      loaded-graphs))
                            (into [])))
                     graphs)
              (state/reset-projects projects)


              (println "Registering watch function over *app-state")
              (state/bootstrapped!)
              ;; Removing any existing watch ensures that we never have more than one watch
              ;; at any time.
              (remove-watch *app-state :storage)
              (add-watch *app-state :storage
                         (fn [key ref old new]
                           (sync-graphs-throttled (:graphs new))))))))



