(ns apibot.views.paper
  "Deserves a namespace of itself due to all the fucking mess that results from
  cytoscape's javascript integration"
  (:require
    [apibot.graphs :as graphs :refer [graph->cytoscape find-edges]]
    [apibot.mixpanel :refer [track]]
    [apibot.coll :refer [swapr!]]
    [apibot.views.commons :refer [subscribe unsubscribe]]
    [reagent.core :as reagent]
    [apibot.views.dialogs :as dialogs]
    [apibot.state :refer [*selected-node-ids *selected-graph]]
    [apibot.coll :as coll]
    [apibot.util :as util]))

;; ---- Model ----

(defn state-at [this ks]
  (-> (reagent/state this)
      (get-in ks)))

(defn event->id [e]
  (-> e .-target .id))

(defn cyto-node->node [node]
  {:id               (.id node)
   :renderedPosition (-> node .renderedPosition util/js->clj)
   :width            (.renderedWidth node)
   :height           (.renderedHeight node)})

(defn event->node [e]
  (let [node (-> e .-target)]
    (cyto-node->node node)))

(defn cyto-edge->edge [edge]
  (when (and (.source edge) (.target edge))
    (let [source (:renderedPosition (cyto-node->node (.source edge)))
          target (:renderedPosition (cyto-node->node (.target edge)))]
      {:id               (.id edge)
       :renderedPosition {:x (/ (+ (:x source) (:x target)) 2)
                          :y (/ (+ (:y source) (:y target)) 2)}})))

(defn event->edge [e]
  (let [edge (-> e .-target)]
    (cyto-edge->edge edge)))

(defn find-selected-nodes [cy]
  (if-not cy
    []
    (-> (.nodes cy ":selected")
        (.map #(cyto-node->node %))
        (vec))))

(defn find-selected-edges [cy]
  (if-not cy
    []
    (-> (.edges cy ":selected")
        (.map #(cyto-edge->edge %))
        (vec))))

(defn send-graph-updates [cy **selected-graph]
  (let [elements (-> (.json @cy)
                     (js->clj :keywordize-keys true)
                     (:elements))]
    (swapr! @**selected-graph graphs/cytoscape->graph elements)))

;; ---- Views ----

(defn menu-node [*cy selected-nodes *state]
  (let [selected-node (first selected-nodes)
        *show-menu (reagent/cursor *state [:show-menu])]
    (when (and selected-node (= 1 (count selected-nodes)))
      (let [{:keys [renderedPosition width height id]} selected-node
            {:keys [x y]} renderedPosition]
        [:button.btn.btn-link
         {:style          {:left      (+ x (/ width 2))
                           :top       (- y (/ height 2) 30)
                           :padding   "0 12px"
                           :margin    0
                           :font-size "25px"
                           :position  "absolute"
                           :z-index   1000000}
          :on-mouse-enter #(reset! *show-menu (:id selected-node))
          :on-mouse-leave #(reset! *show-menu nil)}
         [:span.glyphicon.glyphicon-plus-sign]
         [:ul.dropdown-menu
          {:style          {:display (if (= (:id selected-node) @*show-menu)
                                       "block" "none")
                            :margin-top "-2px"}}
          [:li>a
           {:on-click #(.edgehandles @*cy "start" id)}
           [:span.glyphicon.glyphicon-share-alt]
           " Connect"]
          [:li>a
           {:on-click #(swapr! *selected-graph graphs/remove-nodes-by-id id)}
           [:span.glyphicon.glyphicon-trash]
           " Remove"]
          [:li>a
           {:on-click #(swapr! *selected-graph graphs/duplicate-node id)}
           [:span.glyphicon.glyphicon-copy]
           " Clone"]
          [:li>a
           {:on-click #(swapr! *selected-graph graphs/disconnect-node id)}
           [:span.glyphicon.glyphicon-scissors]
           " Disconnect"]]]))))

(defn menu-edge [*cy selected-edges *state]
  (let [selected-edge (first selected-edges)]
    (when selected-edge
      (let [{:keys [renderedPosition width height id]} selected-edge
            {:keys [x y]} renderedPosition]
        [:button.btn.btn-link
         {:style    {:left      (- x (/ width 2))
                     :top       (- y (/ height 2))
                     :padding   0
                     :margin    0
                     :font-size "25px"
                     :position  "absolute"
                     :z-index   1000000}
          :on-click (fn [e]
                      (swapr! *selected-graph graphs/remove-edges-by #(= (:id %) id)))}
         [:span.glyphicon.glyphicon-trash]]))))


;; ---- View Config ----

(def color-assert-body "#f0ad4e")
(def color-assert-border "#eea236")
(def style-assert {"background-color"   color-assert-body
                   "text-outline-color" color-assert-border
                   "border-color"       color-assert-border})

(def color-extract-body "#5bc0de")
(def color-extract-border "#46b8da")
(def style-extract {"background-color"   color-extract-body
                    "text-outline-color" color-extract-border
                    "border-color"       color-extract-border})

(def color-http-body "#5cb85c")
(def color-http-border "#4cae4c")
(def style-http {"background-color"   color-http-body
                 "text-outline-color" color-http-border
                 "border-color"       color-http-border})

(def style-config
  {"background-color"   "#fff"
   "text-outline-color" "#ddd"
   "color"              "#333"
   "border-color"       "#ddd"})

(def style-custom
  {"background-color"   "#337ab7"
   "text-outline-color" "#2e6da4"
   "border-color"       "#2e6da4"})

(def style
  [{"selector" "node"
    "style"    {"label"              "data(text)"
                "background-color"   "#666"
                "border-width"       2
                "opacity"            "1"
                "color"              "#eee"
                "shape"              "roundrectangle"
                "width"              "label"
                "height"             "label"
                "padding"            "4px"
                "font-size"          "12px"
                "text-wrap"          "wrap"
                "font-family"        "monospace"
                "text-max-width"     "150px"
                "text-valign"        "center"
                "text-outline-width" "2px"
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
                "width"              "2px"
                "line-color"         "#aaa"
                "target-arrow-color" "#aaa"}}
   {"selector" "edge:selected"
    "style"    {"line-color"         "#337ab7"
                "target-arrow-color" "#337ab7"}}
   {"selector" "node:selected"
    "style"    {"border-color" "#333"}}])

(def layout
  {"name" "preset"})

;; ---- Cytoscape General Configuration ----
(defn cytoscape-config [id **selected-graph custom-layout]
  {"container" (-> js/document (.getElementById id))
   "elements"  (graph->cytoscape @@**selected-graph)
   "style"     (clj->js style)
   "layout"    (clj->js (or custom-layout layout))
   "minZoom"   0.2
   "maxZoom"   3})


(defn image-from-url [url]
  (let [img (new js/Image url)]
    (aset img "src" url)
    (aset img "width" 8)
    (aset img "height" 8)
    img))

;; ---- Edge Handles Plugin Configuration ----
(defn edgehandles-config [cy **selected-graph]
  {;; If you hover over a node and then leave the node, don't create an edge.
   :toggleOffOnLeave   true

   ;; Where should the edgehandle be drawn?
   :handlePosition     "middle bottom"
   :handleIcon         (image-from-url "/img/move.svg")

   :handleColor        "#337ab7"
   :handleOutlineColor "#2e6da4"
   :handleOutlineWidth 1

   ;; Can return 'flat' for flat edges between nodes or 'node' for intermediate node between them
   ;; returning null/undefined means an edge can't be added between the two nodes
   :edgeType           (fn [source-node target-node]
                         (if (graphs/connected? (.id source-node) (.id target-node) @@**selected-graph)
                           nil "flat"))

   ;; for the specified node, return whether edges from itself to itself are allowed
   :loopAllowed        (fn [node] false)

   ;; this handler is executed when the edge is susccessfuly added
   :complete           (fn [source-node target-node added-entities]
                         (swapr! @**selected-graph graphs/conj-edge
                                 {:id     (graphs/uuid!)
                                  :source (.id source-node)
                                  :target (.id target-node)}))})

;; ---- Initialize the Cytoscape Graph ----

(defn- init-cytoscape [this id cy **selected-graph layout]
  (let [cyto (js/cytoscape (clj->js (cytoscape-config id **selected-graph layout)))]

    ;; Initializing the edgehandles extension
    (.edgehandles cyto (clj->js (edgehandles-config cy **selected-graph)))

    (.on cyto "select" "node"
         (fn [e]
           (let [node-id (event->id e)]
             (swap! *selected-node-ids conj node-id))))

    (.on cyto "render"
         (fn [e]
           (reagent/force-update this true)))

    (.on cyto "unselect" "node"
         (fn [e]
           (let [node-id (event->id e)]
             (swap! *selected-node-ids disj node-id))))

    ;; Whenever a node is "freed" i.e. after dragging, update the position
    (.on cyto "free" "node"
         (fn [e]
           (send-graph-updates cy **selected-graph)))

    (subscribe :fit-graph id (fn [_] (.fit cyto)))
    (subscribe :format-graph (str "format-graph-" id)
               (fn [_]
                 (swap! @**selected-graph graphs/prettify)
                 (.fit cyto)))

    (reset! cy cyto)))

;; ---- Views ----

(defn create-paper-class [id & {:keys [layout]}]
  (let [;; ---- State ----
        ;; Hold the cytoscape atom here, This will only be available after the
        ;; component has been mounted.
        cy (atom nil)
        ;; A reference to the ID of the current selected graph, this is mostly meant as a means
        ;; for determining when the graph being displayed changed.
        *selected-graph-id (atom nil)
        ;; Hold an an atom of the selected graph's atom.
        ;; Yes, you read correctly, this is an (atom (atom graph))
        **selected-graph (atom nil)]

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
             (reset! *selected-node-ids #{})
             (.fit @cy))

           (when (graphs/singleton? new-graph)
             (.fit @cy))
           ;; return false indicating that this component doesn't need to update
           true))

       :component-did-mount
       (fn [this]
         (init-cytoscape this id cy **selected-graph layout))

       :component-will-unmount
       (fn [this]
         (unsubscribe id))

       :reagent-render
       (fn [selected-graph-ratom]
         (reset! **selected-graph selected-graph-ratom)

         (let [*state (reagent/state-atom (reagent/current-component))]
           [:div
            {:id id :style {:width "100%" :height "100%"}}
            (menu-edge cy (find-selected-edges @cy) *state)
            (menu-node cy (find-selected-nodes @cy) *state)]))})))

(defn create-readonly-paper-class [id & {:keys [layout]}]
  (let [;; ---- State ----
        ;; Hold the cytoscape atom here, This will only be available after the
        ;; component has been mounted.
        cy (atom nil)
        *selected-graph (atom nil)]

    (reagent/create-class
      {:should-component-update
       (fn [this [_ *old-graph] [_ *new-graph]]
         (let [new-graph @*new-graph]
           ;; HACK:
           ;; always prevent component updates and do the updates manually
           ;; here.
           (let [cyto-graph (clj->js {:elements (graph->cytoscape new-graph)})]
             (.json @cy cyto-graph))

           ;; if the user is switching graphs, fit it to the screen!
           ;; and unselect all nodes.
           (.fit @cy)

           ;; return false indicating that this component doesn't need to update
           true))

       :component-did-mount
       (fn [this]
         (init-cytoscape this id cy (atom *selected-graph) layout))

       :component-will-unmount
       (fn [this]
         (unsubscribe id))

       :reagent-render
       (fn [selected-graph-ratom]
         (reset! *selected-graph @selected-graph-ratom)
         [:div
          {:id id :style {:width "100%" :height "100%"}}])})))
