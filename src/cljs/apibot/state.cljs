(ns apibot.state
  (:require
    [reagent.core :refer [atom]]
    [apibot.graphs :as graphs]
    [apibot.util :as util]))


(def sample-fetch-luke-skywalker
  (graphs/map->CustomGraph
    {:id         "sample-fetch-luke-skywalker-0"
     :name       "sample: fetch luke skywalker"
     :desc       "This sample graph shows the basics of making an HTTP request and asserting over the HTTP response."
     :executable true
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
                   :props    {:url "${root}/people/1"}
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
  (graphs/map->CustomGraph
    {:id        "sample-fetch-dagobah-0"
     :desc       "This sample graph shows you how to chain requests and extract content from the request to create powerful assertions."
     :name       "sample: fetch dagobah"
     :executable true
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
                   :props    {:url "${root}/planets/"}}
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
                   :props    {:url "${root}/planets/5"}}
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
  (atom
    {:graphs     []
     :executions {}
     :ui         {:selected-graph-id     nil
                  :tasks-dialog-expanded true
                  :bootstrapped          false}}))

(defn bootstrapped? []
  (get-in @*app-state [:ui :bootstrapped]))

(defn bootstrapped! []
  (util/reset-in! *app-state [:ui :bootstrapped] true))
