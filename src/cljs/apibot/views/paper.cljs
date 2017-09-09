(ns apibot.views.paper
  "Deserves a namespace of itself due to all the fucking mess that results from
  cytoscape's javascript integration"
  (:require
    [apibot.graphs :as graphs :refer [graph->cytoscape find-edges]]
    [apibot.mixpanel :refer [track]]
    [apibot.coll :refer [swapr!]]
    [apibot.views.commons :refer [subscribe unsubscribe]]
    [reagent.core :as reagent :refer [atom create-class props]]
    [apibot.views.dialogs :as dialogs]
    [apibot.coll :as coll]))

;; ---- Model ----

(defn event->id [e]
  (-> e .-cyTarget .id))

(defn send-graph-updates [cy **selected-graph]
  (let [elements (-> (.json @cy)
                     (js->clj :keywordize-keys true)
                     (:elements))]
    (swapr! @**selected-graph graphs/cytoscape->graph elements)))

(defn duplicate-node
  "Given a cytoscape ele as argument, it clones the node and adds it to the graph."
  [ele]
  (track :ev-paper-duplicate-node)
  (let [cloned (.clone ele)
        new-text (str (.data cloned "text") " - COPY")
        new-id (graphs/uuid!)
        rand (rand-int 10)
        new-x (+ (.position cloned "x") 20 rand)
        new-y (+ (.position cloned "y") 20 rand)]
    (-> cloned
        (.json)
        (js->clj)
        (assoc-in ["data" "id"] new-id)
        (assoc-in ["data" "text"] new-text)
        (assoc-in ["position" "x"] new-x)
        (assoc-in ["position" "y"] new-y)
        (assoc-in ["data" "node" "id"] new-id)
        (assoc-in ["data" "node" "name"] new-text)
        (assoc-in ["data" "node" "position" "x"] new-x)
        (assoc-in ["data" "node" "position" "y"] new-y)
        (clj->js))))

;; ---- View Config ----

(def color-assert-body "#f0ad4e")
(def color-assert-border "#eea236")
(def style-assert {"background-color" color-assert-body
                   "border-color"     color-assert-border})

(def color-extract-body "#5bc0de")
(def color-extract-border "#46b8da")
(def style-extract {"background-color" color-extract-body
                    "border-color"     color-extract-border})

(def color-http-body "#5cb85c")
(def color-http-border "#4cae4c")
(def style-http {"background-color" color-http-body
                 "border-color"     color-http-border})

(def style-config
  {"background-color" "#fff"
   "border-color"     "#ddd"})

(def style-custom
  {"background-color" "#337ab7"
   "border-color" "#2e6da4"})

(def style
  [{"selector" "node"
    "style"    {"label"              "data(text)"
                "background-color"   "#666"
                "border-width"       2
                "opacity"            "0.95"
                "font-size"          "10px"
                "text-wrap"          "wrap"
                "text-max-width"     "100px"
                "text-valign"        "center"
                "text-outline-width" 1
                "text-outline-color" "#888"
                "color"              "white"
                "z-index"            0}}

   {"selector" "node[type = 'custom']"
    "style"    style-custom}
   {"selector" "node[type = 'config']"
    "style"    style-config}
   {"selector" "node[type = 'csv']"
    "style"    style-config}
   {"selector" "node[type = 'http-request']"
    "style"    style-http}
   {"selector" "node[type = 'assert']"
    "style"    style-assert}
   {"selector" "node[type = 'assert-status']"
    "style"    style-assert}
   {"selector" "node[type = 'assert-body']"
    "style"    style-assert}
   {"selector" "node[type = 'assert-headers']"
    "style"    style-assert}
   {"selector" "node[type = 'eval']"
    "style"    style-extract}
   {"selector" "node[type = 'evaljs']"
    "style"    style-extract}
   {"selector" "node[type = 'extract-body']"
    "style"    style-extract}
   {"selector" "node[type = 'extract-header']"
    "style"    style-extract}
   {"selector" "edge"
    "css"      {"target-arrow-shape" "triangle"
                "curve-style"        "bezier"
                "background-color"   "#9d9d9d"}}
   {"selector" ":selected"
    "style"    {"border-color" "#333"}}])

(def layout
  {"name" "preset"})

;; ---- Cytoscape General Configuration ----
(defn cytoscape-config [id selected-graph custom-layout]
  {"container" (-> js/document (.getElementById id))
   "elements"  (graph->cytoscape @@selected-graph)
   "style"     (clj->js style)
   "layout"    (clj->js (or custom-layout layout))
   "minZoom"   0.2
   "maxZoom"   3})

;; ---- Context Menu Configuration ----
(defn cxt-config [cy selected-graph]
  {:menuRadius          80                                  ;; the radius of the circular menu in pixels
   :selector            "node, edge"                        ;; elements matching this Cytoscape.js selector will trigger cxtmenus
   :commands
                        [{:content "Remove"
                          :select
                                   (fn [ele]
                                     (track :ev-paper-remove)
                                     (.remove @cy (str "#" (.id ele)))
                                     (send-graph-updates cy selected-graph))}

                         {:content "Clone"
                          :select
                                   (fn [ele]
                                     (track :ev-paper-clone)
                                     (if (= "nodes" (.group ele))
                                       (let [node (duplicate-node ele)]
                                         (.add @cy node)
                                         (send-graph-updates cy selected-graph))
                                       (dialogs/show!
                                         (dialogs/message-dialog
                                           "Oopsie!"
                                           "You were trying to clone an edge but only nodes can be cloned."))))}

                         {:content "Disconnect"
                          :select
                                   (fn [ele]
                                     (track :ev-paper-disconnect)
                                     (if (= "nodes" (.group ele))
                                       (let [edges (find-edges (fn [{:keys [source target]}]
                                                                 (or (= source (.id ele))
                                                                     (= target (.id ele))))
                                                               @@selected-graph)
                                             edge-ids (map :id edges)]
                                         (doseq [edge-id edge-ids]
                                           (.remove @cy (str "#" edge-id)))
                                         (send-graph-updates cy selected-graph))
                                       (do
                                         (.remove @cy (str "#" (.id ele)))
                                         (send-graph-updates cy selected-graph))))}]

   ;; the background colour of the menu
   :fillColor           "rgba(51, 51, 51, 0.75)"

   ;; the colour used to indicate the selected command
   :activeFillColor     "rgba(51, 122, 183, 0.75)"

   ;; additional size in pixels for the active command
   :activePadding       5

   ;; the size in pixels of the pointer to the active command
   :indicatorSize       20

   ;; the empty spacing in pixels between successive commands
   :separatorWidth      2

   ;; extra spacing in pixels between the element and the spotlight
   :spotlightPadding    4

   ;; the minimum radius in pixels of the spotlight
   :minSpotlightRadius  24

   ;; the maximum radius in pixels of the spotlight
   :maxSpotlightRadius  38

   ;; space-separated cytoscape events that will open the menu; only `cxttapstart` and/or `taphold` work here
   :openMenuEvents      "taphold"

   ;; the colour of text in the command's content
   :itemColor           "white"

   ;; the text shadow colour of the command's content
   :itemTextShadowColor "black"

   ;; the z-index of the ui div
   :zIndex              9999

   ;; draw menu at mouse position
   :atMouse             false})

(defn image-from-url [url]
  (let [img (new js/Image url)]
    (aset img "src" url)
    (aset img "width" 8)
    (aset img "height" 8)
    img))

;; ---- Edge Handles Plugin Configuration ----
(defn edgehandles-config [cy selected-graph]
  {;; If you hover over a node and then leave the node, don't create an edge.
   :toggleOffOnLeave   true

   ;; Where should the edgehandle be drawn?
   :handlePosition     "middle bottom"
   :handleIcon         (image-from-url "/img/move.svg")

   :handleColor        "#337ab7"
   :handleOutlineColor "#2e6da4"
   :handleOutlineWidth 1

   ;; for the specified node, return whether edges from itself to itself are allowed
   :loopAllowed        (fn [node] false)

   ;; this handler is executed when the edge is susccessfuly added
   :complete           (fn [source-node target-node added-entities]
                         (send-graph-updates cy selected-graph))})

;; ---- Initialize the Cytoscape Graph ----

(defn- init-cytoscape [id cy **selected-graph layout]
  (let [cyto (js/cytoscape (clj->js (cytoscape-config id **selected-graph layout)))]
    ;; Initialize the cxtmenu extension
    (.cxtmenu cyto (clj->js (cxt-config cy **selected-graph)))

    ;; Initializing the edgehandles extension
    (.edgehandles cyto (clj->js (edgehandles-config cy **selected-graph)))

    ;; Register an on select node handler
    (.on cyto "select" "node"
         (fn [e]
           (let [node-id (event->id e)]
             (coll/swapr! @**selected-graph graphs/set-selected-node node-id true))))

    #_(.on cyto "select" "edge"
         (fn [e]
           (send-graph-updates cy **selected-graph)))

    ;; Register an on unselect node handler
    (.on cyto "unselect" "node"
         (fn [e]
           (let [node-id (event->id e)]
             (coll/swapr! @**selected-graph graphs/set-selected-node node-id false))))

    ;; Whenever a node is "freed" i.e. after dragging, update the position
    (.on cyto "free" "node"
         (fn [e]
           (send-graph-updates cy **selected-graph)))

    (subscribe :fit-graph id (fn [_] (.fit cyto)))

    (reset! cy cyto)))

;; ---- Views ----

(defn create-paper-class [id & {:keys [layout]}]
  (let [;; ---- State ----
        ;; Hold the cytoscape atom here, This will only be available after the
        ;; component has been mounted.
        cy (reagent/atom nil)
        ;; A reference to the ID of the current selected graph, this is mostly meant as a means
        ;; for determining when the graph being displayed changed.
        *selected-graph-id (atom nil)
        ;; Hold an an atom of the selected graph's atom.
        ;; Yes, you read correctly, this is an (atom (atom graph))
        **selected-graph (reagent/atom nil)]

    (reagent/create-class
      {:should-component-update
       (fn [this [_ *old-graph] [_ *new-graph]]
         (let [new-graph @*new-graph]
           ;; HACK:
           ;; always prevent component updates and do the updates manually
           ;; here.
           (reset! **selected-graph *new-graph)
           (let [cyto-graph (clj->js {:elements (graph->cytoscape new-graph)})]
             (.json @cy cyto-graph))

           ;; if the user is switching graphs, fit it to the screen!
           ;; and unselect all nodes.
           (when (not= @*selected-graph-id (:id new-graph))
             (reset! *selected-graph-id (:id new-graph))
             (swapr! *new-graph graphs/unselect-nodes)
             (.fit @cy))

           (when (graphs/singleton? new-graph)
             (.fit @cy))
           ;; return false indicating that this component doesn't need to update
           false))

       :component-did-mount
       (fn [this]
         (init-cytoscape id cy **selected-graph layout))

       :component-will-unmount
       (fn [this]
         (unsubscribe id))

       :reagent-render
       (fn [selected-graph-ratom]
         (reset! **selected-graph selected-graph-ratom)
         [:div {:id id :style {:width "100%" :height "100%"}}])})))
