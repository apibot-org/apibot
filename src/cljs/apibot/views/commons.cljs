(ns apibot.views.commons
  "A namespace for reusable higher level components used throughout apibot"
  (:require
    [cljs.spec.alpha :as s]
    [apibot.coll :as coll]
    [reagent.core :as reagent :refer [atom cursor]]
    [promesa.core :as p]
    [apibot.graphs :as graphs]
    [clojure.string :as str]))

(defn warning-sign [title message]
  [:p.alert.alert-warning
   {:role  "alert"
    :style {:margin-top "4px"}}
   [:span.glyphicon.glyphicon-exclamation-sign]
   [:b " " title]
   message])

(defn conditional-classes [m]
  (->> (filter (fn [[css-class bool]] bool) m)
       (map (fn [[css-class bool]] css-class))
       (str/join " ")))

(defn http-status->label [status]
  (cond
    (coll/in-range? status 100 200) "label-default"
    (coll/in-range? status 200 300) "label-success"
    (coll/in-range? status 300 400) "label-info"
    (coll/in-range? status 400 500) "label-warning"
    :else "label-danger"))

(defn graph-name
  "A simple helper method for displaying a graph's name and defaulting to 'no name' in case
  there is no name."
  [graph]
  (coll/or-empty? (graphs/label graph) [:i "no name"]))

(defn glyphicon-run []
  [:span.glyphicon.glyphicon-flash
   {:aria-hidden "true"
    :style       {:color       "#DFBA69"
                  :text-shadow "1px 1px rgba(255,255,255,0.3)"}}])

(defn button-swagger [props text]
  []
  [:button.btn.btn-success
   (merge {:type  "button"
           :style {:background-image    "url('img/logo-swagger.png')"
                   :background-repeat   "no-repeat"
                   :padding-left        "36px"
                   :background-size     "25px"
                   :background-position "4px 3px"}}
          props)
   text])

(defn promise-view
  "Displays progress on a promise. Updates itself when the promise is done.
  view-done and view-failed can be either views e.g. [:p 'done'] or functions
  which take the result of the promise as argument and return a view e.g.
  (fn [result] [:p 'Number of cows: ' result])

  See also commons/bind-promise!."
  ([*bound-promise view-pending view-done]
   (promise-view *bound-promise view-pending view-done view-done))
  ([*bound-promise view-pending view-done view-failed]
   (let [{:keys [state value]} @*bound-promise
         wrap-as-fn (fn [view-or-func value]
                      (if (fn? view-or-func)
                        (view-or-func value)
                        view-or-func))]
     (cond
       (= state :pending)
       (wrap-as-fn view-pending value)
       (= state :done)
       (wrap-as-fn view-done value)
       (= state :error)
       (wrap-as-fn view-failed value)
       :else
       (throw (ex-info "Unknown bound-promise state:" state))))))


(defn input-bindable
  "An attempt at a data-bindable input view.
  - html-tag: defaults to :input
  - args: argument map passed onto reagent.
  - ratom: a reagent atom."
  ([html-tag args ratom]
   (let [transform (get args :transform identity)
         ;; Create an on change handler which reset!s the cursor with
         ;; the changed value
         on-change-handler (fn [e] (reset! ratom (transform (-> e .-target .-value))))
         ;; Provide some defaults and merge with the given args.
         new-args (merge {:value     (or @ratom "")
                          :on-change on-change-handler}
                         (dissoc args :transform))]
     [html-tag new-args]))
  ([args ratom]
   (input-bindable :input (merge {:type "text"} args) ratom)))

(defn form-group-bindable
  "Creates a bootstrap form-group with binding for a given field using
  input-bindable.

  Special arguments:
  - :spec (optional) A spec can be supplied to add form validation
  - :name The field's name e.g. 'phone number'
  - :help (optional) An optional help label (uses .help-block)
  - :transform (optional) A function that takes the forms input as argument and transforms it
                          useful for parsing numbers or keywords from the raw text input.
  "
  ([html-tag args ratom]
   (let [field-name (:name args)
         field-id (str (gensym field-name))
         spec (:spec args)
         help-text (:help args)
         ;; Verify if the ratom conforms to the spec.
         opts (if (or (nil? spec) (s/valid? spec @ratom))
                {:class ""}
                {:class "has-error"})]

     [:div.form-group opts
      [:label {:for field-id :class "control-label"} field-name]
      [input-bindable
       html-tag
       (merge {:class "form-control" :id field-id :placeholder field-name}
              (dissoc args :spec :help))
       ratom]
      (when help-text
        [:p.help-block help-text])]))
  ([args ratom]
   (form-group-bindable :input args ratom)))

(defn cursor-vec
  [*coll-atom path]
  (->> (get-in @*coll-atom path [])
       (count)
       (range)
       (map (fn [index] (reagent/cursor *coll-atom (conj path index))))
       (into [])))

(defn cursor-map
  "Takes a derefable *obj and a vector as a path that leads to a map.
  Returns a vector of cursors for each element in the map."
  [*obj path-to-map]
  (->> (get-in @*obj path-to-map [])
       (keys)
       (reduce (fn [result-map key]
                 (assoc result-map key
                                   (reagent/cursor *obj (conj path-to-map key))))
               {})))

(defn find-as-cursor
  "returns a ratom or nil if no element matches the predicate.

  Returns a cursor roughly equivalent to (cursor *app-state [ks... n])
  where n is the first item in (get-in *app-state ks) such that (pred item) is
  true.

  If no element matches the predicate, returns nil (not a cursor with nil but
  actually nil)"
  [*app-state ks pred]
  (let [pos (->> (get-in @*app-state ks)
                 (coll/positions pred)
                 (first))]
    (when pos
      (cursor *app-state (into ks [pos])))))

(defn find-as-cursors
  "Assuming that the path app-state > ks leads to a vector, returns a coll of cursors s.t.
  each cursor maps to an item in the vector which matches the predicate pred."
  [*app-state ks pred]
  (assert (vector? ks))
  (->> (get-in @*app-state ks)
       (coll/positions pred)
       (map #(cursor *app-state (conj ks %)))))

;; ---- Pub Sub ----

(def listeners (clojure.core/atom {}))

(defn subscribe
  [channel id handler]
  {:pre [(keyword? channel) (string? id) (fn? handler)]}
  (swap! listeners assoc id {:channel channel :handler handler}))

(defn unsubscribe
  [id]
  {:pre [(string? id)]}
  (swap! listeners dissoc id))

(defn publish
  [channel obj]
  (doseq [listener (vals @listeners)]
    (when (= channel (:channel listener))
      ((:handler listener) obj))))

;; ---- End Pub Sub ----
