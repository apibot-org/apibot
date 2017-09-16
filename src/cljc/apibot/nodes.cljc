(ns apibot.nodes
  (:require
    [clojure.string :refer [split join starts-with? replace-first]]
    [apibot.coll :as coll]))


(defmulti label :type)

(defn path-preview
  "Takes a string representing a url or templated url and returns the last 3 parts from the path.
  Example:

  (path-preview '${roo}/bar/baz')
  => '/bar/baz'
  (path-preview '#'
  "
  [string]
  (let [prev (->> (split string #"/")
                  (filter (complement #{"" "/" "http:" "https:"}))
                  (filter #(not (re-find #"\$\{\w+\}" %)))
                  (reverse)
                  (coll/subcoll 3)
                  (reverse)
                  (join "/"))]
    (if (starts-with? prev "/")
      prev
      (str "/" prev))))

(defmethod label "http-request" [node]
  (let [{:keys [http-method url]} (:props node)]
    #?(:clj url
       :cljs (try
               (let [pathname (-> (new js/URL url) .-pathname)]
                 (str http-method " " (path-preview pathname)))
               (catch :default e
                 (str http-method " " (path-preview url)))))))


(defmethod label "assert-status" [node]
  (let [{:keys [from to]} (:props node)]
    (cond
      (or (not from) (not to))
      (str "assert status [" (or from "?") "," (or to "?") "]")

      (not (number? from))
      (str "<invalid from>")

      (not (number? to))
      (str "<invalid to>")

      (= from to)
      (str "assert status " from)

      (and (= from 200) (= to 299))
      "assert status 2xx"

      (and (= from 300) (= to 399))
      "assert status 3xx"

      (and (= from 400) (= to 499))
      "assert status 4xx"

      (and (= from 500) (= to 599))
      "assert status 5xx"

      :else
      (str "assert status [" from "," to "]"))))

(defmethod label "extract-header" [node]
  (let [{:keys [name header]} (:props node)]
    (str "extract " (or header "?")
         " as " (or name "?"))))

(defmethod label "extract-body" [node]
  (let [{:keys [name]} (:props node)]
    (str "extract " (or name "?")
         " from body")))

(defmethod label :default [node]
  (:name node))

(defn node->cyto [node]
  "Converts a node into a cytoscape node. If the node's position is not defined,
  then the node is placed at a randomly chosen renderedPosition close to the
  top left corner."
  (let [position (:position node)
        cyto-node {:group "nodes"
                   :locked false
                   :grabbable true
                   :selectable true
                   :data  {:id   (:id node)
                           :text (label node)
                           :type (:type node)
                           :node node}}
        pos-key (if position :position :renderedPosition)
        pos-val (or position {:x (+ (rand-int 20) 90)
                              :y (+ (rand-int 20) 90)})]
    (assoc cyto-node pos-key pos-val)))
