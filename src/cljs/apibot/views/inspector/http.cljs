(ns apibot.views.inspector.http
  "An inspector component for making HTTP requests"
  (:require
    [apibot.grexec.http-node :as http-node]
    [apibot.util :refer [remove-element-at]]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons :refer [form-group-bindable input-bindable cursor-vec]]
    [clojure.string :refer [lower-case trim]]
    [cljs.spec.alpha :as s]
    [reagent.core :as reagent :refer [cursor]]))

;; ---- Model ----

(defn add-key-val [*keyvals key-val]
  (swap! *keyvals
         #(conj (or % []) key-val)))

(defn button-add-new-key-val
  [*values]
  [:a {:role     "button"
       :on-click (fn [e] (add-key-val *values {:key "" :val ""}))}
   " Add new"])

(defn find-content-type [props]
  (->> props
       :headers
       (filter (fn [{:keys [key]}] (-> key lower-case trim (= "content-type"))))
       (map :val)
       (first)))


;; ---- Views ----

(def editor-json
  (create-editor
    {:id     "body-editor"
     :class  "form-control"
     :editor {:language :json}
     :style  {:padding "0px"
              :height  "250px"}}))

(def editor-html
  (create-editor
    {:id     "body-editor"
     :class  "form-control"
     :editor {:language :html}
     :style  {:padding "0px"
              :height  "250px"}}))

(def editor-xml
  (create-editor
    {:id     "body-editor"
     :class  "form-control"
     :editor {:language :xml}
     :style  {:padding "0px"
              :height  "250px"}}))

(defn form-group-key-val-pair
  [*keyvals index *keyval]
  (let [opts (if (s/valid? ::http-node/header @*keyval)
               {} {:class "has-error"})]
    [:div.form-group opts
     [:div.row
      [:div.col-xs-6 {:style {:padding-right "2px"}}
       [input-bindable
        {:type        "text"
         :class       "form-control"
         :placeholder "key name e.g. 'rootUrl'"}
        (cursor *keyval [:key])]]
      [:div.col-xs-6 {:style {:padding-left "2px"}}
       [:div.input-group
        [input-bindable
         {:type        "text"
          :class       "form-control"
          :placeholder "key name e.g. 'rootUrl'"}
         (cursor *keyval [:val])]
        [:span.input-group-btn
         [:button.btn.btn-default
          {:type     "button"
           :on-click (fn [e] (swap! *keyvals remove-element-at index))}
          "X"]]]]]]))

(defn http
  [node-ratom]
  (let [*headers (cursor node-ratom [:props :headers])
        *http-method (cursor node-ratom [:props :http-method])
        header-forms
        (->> (cursor-vec node-ratom [:props :headers])
             (map-indexed
               (fn [index header-ratom]
                 ^{:key index} [form-group-key-val-pair
                                *headers
                                index
                                header-ratom])))
        *query-params (cursor node-ratom [:props :query-params])
        query-param-forms
        (->> (cursor-vec node-ratom [:props :query-params])
             (map-indexed
               (fn [index *query-param]
                 ^{:key index} [form-group-key-val-pair
                                *query-params
                                index
                                *query-param])))
        *body (cursor node-ratom [:props :body])]
    [:form
     [:div.help-block
      [commons/link-docs "http-request"]]
     [form-group-bindable {:name "Name"} (cursor node-ratom [:name])]
     [form-group-bindable
      {:name "Url"
       :spec ::http-node/url}
      (cursor node-ratom [:props :url])]

     [:label "Query Params "]
     query-param-forms
     [button-add-new-key-val *query-params]

     [:div.form-group
      [:label "Method"]
      [:select.form-control
       {:on-change #(reset! *http-method (-> % .-target .-value))
        :value (if (empty? @*http-method) "GET" @*http-method)}
       [:option {:value "GET"} "GET"]
       [:option {:value "POST"} "POST"]
       [:option {:value "PUT"} "PUT"]
       [:option {:value "DELETE"} "DELETE"]
       [:option {:value "PATCH"} "PATCH"]
       [:option {:value "OPTIONS"} "OPTIONS"]
       [:option {:value "HEAD"} "HEAD"]
       [:option {:value "CONNECT"} "CONNECT"]]]

     (let [content-type (find-content-type (:props @node-ratom))]
       (cond
         (= content-type "application/json")
         [:div.form-group
          [:label {:for "body-editor" :class "control-label"} "Body (JSON)"]
          [:div [editor-json *body]]]

         (= content-type "text/html")
         [:div.form-group
          [:label {:for "body-editor" :class "control-label"} "Body (HTML)"]
          [:div [editor-html *body]]]

         (= content-type "application/xml")
         [:div.form-group
          [:label {:for "body-editor" :class "control-label"} "Body (XML)"]
          [:div [editor-xml *body]]]

         (some? content-type)
         [form-group-bindable
          :textarea
          {:name "Body" :rows 3}
          *body]

         :else
         [:div.alert.alert-info {:role "alert"}
          [:b "No Content Type: "]
          "Please enter a "
          [:a {:role     "button"
               :on-click (fn [e]
                           (add-key-val *headers
                                        {:key "content-type"
                                         :val "application/json"}))}
           [:b "content-type"]]
          " header to enable the body editor."]))

     [:label "Headers "]
     header-forms
     [button-add-new-key-val *headers]]))
