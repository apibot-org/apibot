(ns apibot.state
  (:require
    [apibot.coll :as coll]
    [apibot.graphs :as graphs]
    [apibot.views.commons :as commons]
    [reagent.core :refer [atom cursor track]]))


(def sample-fetch-luke-skywalker
  (graphs/create-custom-graph
    {:id         (graphs/uuid!)
     :name       "sample: fetch luke skywalker"
     :desc       "This sample graph shows the basics of making an HTTP request and asserting over the HTTP response."
     :executable true
     :projects   (set [])
     :edges      [{:id     "sample-fetch-luke-skywalker-edge-0"
                   :source "sample-fetch-luke-skywalker-node-config"
                   :target "sample-fetch-luke-skywalker-node-http-request"}
                  {:id     "sample-fetch-luke-skywalker-edge-1"
                   :source "sample-fetch-luke-skywalker-node-http-request"
                   :target "sample-fetch-luke-skywalker-node-assert-body"}]
     :nodes      [{:graph-id "config"
                   :id       "sample-fetch-luke-skywalker-node-config"
                   :name     "Config"
                   :position {:x 100
                              :y 100}
                   :props    {:config [{:key "root"
                                        :val "https://swapi.co/api"}]}
                   :type     "config"}
                  {:graph-id "http-request"
                   :id       "sample-fetch-luke-skywalker-node-http-request"
                   :name     "HTTP Request"
                   :position {:x 100
                              :y 180}
                   :props    {:url "${root}/people/1" :http-method "GET"}
                   :type     "http-request"}
                  {:graph-id "assert-body"
                   :id       "sample-fetch-luke-skywalker-node-assert-body"
                   :name     "Assert Body"
                   :position {:x 100
                              :y 260}
                   :props    {:fn       "(body) => {\n  return body.name == \"Luke Skywalker\";\n}"
                              :template "Expected the user to be luke"}
                   :type     "assert-body"}]}))

(def sample-fetch-dagobah
  (graphs/create-custom-graph
    {:id         (graphs/uuid!)
     :desc       "This sample graph shows you how to chain requests and extract content from the request to create powerful assertions."
     :name       "sample: fetch dagobah"
     :executable true
     :projects   (set [])
     :nodes
                 [{:name     "Config"
                   :type     "config"
                   :id       "sample-fetch-luke-skywalker-node-config"
                   :graph-id "config"
                   :position {:y 100, :x 100}
                   :props    {:config [{:key "root", :val "https://swapi.co/api"}]}}
                  {:name     "fetch planets"
                   :type     "http-request"
                   :id       "sample-fetch-luke-skywalker-node-http-request"
                   :graph-id "http-request"
                   :position {:y 160, :x 100}
                   :props    {:url "${root}/planets/" :http-method "GET"}}
                  {:name     "Extract the third planet"
                   :type     "extract-body"
                   :id       "9cf90d35-e6ba-4346-8896-caf798f33358"
                   :graph-id "extract-body"
                   :position {:y 220, :x 100}
                   :props    {:fn "(body) => {\n  return body.results[3];\n}", :name "my_planet"}}
                  {:name     "fetch Dagobah"
                   :type     "http-request"
                   :id       "6931f6b9-c9fc-4743-8398-f754a0401815"
                   :graph-id "http-request"
                   :position {:y 280, :x 100}
                   :props    {:url "${root}/planets/5" :http-method "GET"}}
                  {:name     "Assert Body"
                   :type     "assert-body"
                   :id       "9ae8f969-7b77-4941-ab66-c107600ce3dd"
                   :graph-id "assert-body"
                   :position {:y 340, :x 100}
                   :props    {:fn "(body, scope) => {\n  return body.name === scope.my_planet.name;\n}"}}]
     :edges
                 [{:source "sample-fetch-luke-skywalker-node-config"
                   :id     "sample-fetch-luke-skywalker-edge-0"
                   :target "sample-fetch-luke-skywalker-node-http-request"}
                  {:source "sample-fetch-luke-skywalker-node-http-request"
                   :id     "8b4c7668-5a72-4084-9d7e-ec0f517c0752"
                   :target "9cf90d35-e6ba-4346-8896-caf798f33358"}
                  {:source "9cf90d35-e6ba-4346-8896-caf798f33358"
                   :id     "ed29edbe-5a06-48e3-84a1-f9917cb56e91"
                   :target "6931f6b9-c9fc-4743-8398-f754a0401815"}
                  {:source "6931f6b9-c9fc-4743-8398-f754a0401815"
                   :id     "4f629d10-43b0-42c2-a406-3554206291c4"
                   :target "9ae8f969-7b77-4941-ab66-c107600ce3dd"}]}))

(def samples [sample-fetch-luke-skywalker sample-fetch-dagobah])

(def *app-state
  "Stores the whole application's state"
  (atom
    {:graphs                            []
     ; The ID of the current selected graph
     :selected-graph-id                 nil
     ; The IDs of the current selected nodes in the selected graph.
     :selected-node-ids                 #{}
     :executions                        {}
     :execution-history                 []
     :projects                          {"default" {:id "default" :name "Apibot"}}
     :selected-project-id               "default"
     ; A graph ID to filter execution results in the execution history view.
     :execution-history>filter-graph-id nil
     :ui                                {:tasks-dialog-expanded true
                                         :bootstrapped          false}}))


(def *graphs
  "A cursor to the list of graphs"
  (cursor *app-state [:graphs]))


(defn find-graph-cursor
  "Returns a cursor to the graph with the given ID"
  [graph-id]
  (cursor (fn ([_]
               (graphs/find-graph-by-id graph-id @*graphs))
              ([_ graph]
               (coll/swapr! *graphs graphs/update-graph graph)))
          []))



(def *selected-graph
  "A cursor to the current selected graph."
  ;; This cursor makes sure that the two paths are always consistent:
  ;; - :selected-graph
  ;; - the graph in :graphs that matches the :selected-graph
  (cursor (fn ([_]
               (let [{:keys [graphs selected-graph-id]} @*app-state]
                 (graphs/find-graph-by-id selected-graph-id graphs)))
            ([_ new-graph]
             (swap! *app-state
                    (fn [app-state new-selected-graph]
                      (let [{:keys [graphs]} app-state]
                        (assoc app-state :graphs (graphs/update-graph new-selected-graph graphs)
                                         :selected-graph-id (:id new-selected-graph))))
                    new-graph)))
          [:selected-graph]))

(def *selected-node-ids
  "A set with the ID of all selected nodes."
  (cursor *app-state [:selected-node-ids]))

(defn selected-nodes-cursors []
  (commons/find-as-cursors
    *selected-graph [:nodes]
    (fn [node]
      (contains? @*selected-node-ids (:id node)))))

(def *projects
  "A cursor to a map of project ID => project. For a list of projects see *project-list below."
  (cursor *app-state [:projects]))

(defn reset-projects
  "Takes a list of projects as input and updates all projects (preserving the default project)"
  [projects]
  (assert (not (map? projects)) "projects must be a list, not a map")
  (let [project-index (->> projects
                           (filter #(not= (:id %) "default"))
                           (coll/index :id))]
    (swap! *projects (fn [projects-old projects-new]
                       (assoc projects-new "default" (get projects-old "default")))
                     project-index)))

(defn add-project
  "Adds a new empty project with the given name. Returns the created project."
  [name]
  (let [new-project {:name name
                     :id   (graphs/uuid!)}]
    (coll/reset-in! *app-state [:projects (:id new-project)] new-project)
    new-project))


(def *project-list
  "A list of projects owned by the user."
  (track #(vals @*projects)))


(def *selected-project
  "The current selected project"
  (cursor (fn ([_]
               (let [{:keys [projects selected-project-id]} @*app-state]
                 (get projects selected-project-id)))
            ([_ project]
             (swap! *app-state
                    (fn [app-state]
                      (-> (assoc-in app-state [:projects (:id project)] project)
                          (assoc :selected-project-id (:id project)))))))
          []))


(defn reset-selected-graph-by-id! [graph-id]
  (coll/reset-in! *app-state [:selected-graph-id] graph-id))

(def *executions
  "A cursor to the executions which are modelled as a map from graph-id => promise(execution result)."
  (cursor *app-state [:executions]))

(def *execution-history>filter-graph-id
  "A cursor to the graph-id used to filter executions."
  (cursor *app-state [:execution-history>filter-graph-id]))

(defn bootstrapped? []
  (get-in @*app-state [:ui :bootstrapped]))

(defn bootstrapped! []
  (coll/reset-in! *app-state [:ui :bootstrapped] true))
