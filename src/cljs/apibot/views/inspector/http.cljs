(ns apibot.views.inspector.http
  "An inspector component for making HTTP requests"
  (:require
    [apibot.grexec.http-node :as http-node]
    [apibot.storage :as storage]
    [apibot.util :refer [remove-element-at]]
    [apibot.views.code-editor :refer [create-editor]]
    [apibot.views.commons :as commons :refer [form-group-bindable input-bindable cursor-vec]]
    [apibot.views.dialogs :as dialogs]
    [cljs.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.string :refer [lower-case trim]]
    [reagent.core :as reagent :refer [atom cursor]]
    [apibot.util :as util]
    [cats.monad.exception :as exception]))

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

(defn query-match
  [query-string {:keys [url]}]
  (str/includes? url query-string))

(defn dialog-swagger-select-endpoint
  [*node endpoints]
  (let [*query (atom "")]
    (fn []
      [dialogs/generic-dialog
       "Select an endpoint to import"
       [:div
        {:style {:border-radius "4px"}}
        (commons/input-bindable
          :input
          {:placeholder "Search"
           :type        "text"
           :style       {:margin-bottom "4px"}
           :class       "form-control"}
          *query)
        [:div.list-group
         {:style {:max-height "300px"
                  :overflow-y "scroll"
                  :overflow-x "hidden"}}
         (->> endpoints
              (filter #(query-match @*query %))
              (map (fn [endpoint endpoints]
                     (let [{:keys [http-method url]} endpoint
                           props (select-keys endpoint [:http-method :url :headers :body :query-params])]
                       [:button.list-group-item
                        {:key      (str http-method "-" url)
                         :on-click #(do (util/reset-in! *node [:props] props)
                                        (dialogs/hide!))}
                        http-method " " url])))
              (doall))]]
       [:div
        [:button.btn.btn-secondary
         {:type "button" :on-click #(dialogs/hide!)}
         "Cancel"]]])))


(defn dialog-swagger-import
  [*node *swagger-json]
  (let [*swagger-json (atom (storage/get-item :storage-swagger-json ""))
        *parsing-error (atom false)
        parse-endpoints (exception/wrap
                          (fn []
                            (->> @*swagger-json
                                 (util/from-json)
                                 (http-node/parse-swagger))))]
    (fn []
      [dialogs/generic-dialog
       "Import Swagger Endpoint"
       [:form
        [form-group-bindable
         :textarea
         {:name        "Swagger JSON"
          :placeholder "swagger.json"}
         *swagger-json]
        (if @*parsing-error
          [:div.alert.alert-danger {:role "alert"}
           [:b "Parsing Error: "]
           "Unable to parse the provided Swagger JSON, please make sure you are providing JSON."]
          [:p.help-block "Copy and paste the complete swagger .json in the text box above or "
           [:a {:href "http://apibot.co/docs/tutorials/swagger-import" :target "_blank"} "click here for help"]"."])]
       [:div
        [:button.btn.btn-secondary
         {:type "button" :on-click #(dialogs/hide!)}
         "Cancel"]
        [:button.btn.btn-primary
         {:type     "button"
          :on-click (fn [e]
                      (reset! *parsing-error false)
                      (let [result (parse-endpoints)]
                        (if (exception/success? result)
                          (do
                            (dialogs/show! [dialog-swagger-select-endpoint *node (exception/extract result)])
                            (storage/set-item :storage-swagger-json @*swagger-json))
                          (reset! *parsing-error true))))}
         "Import"]]])))

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
  [*node]
  (let [*headers (cursor *node [:props :headers])
        *http-method (cursor *node [:props :http-method])
        header-forms
        (->> (cursor-vec *node [:props :headers])
             (map-indexed
               (fn [index header-ratom]
                 ^{:key index} [form-group-key-val-pair
                                *headers
                                index
                                header-ratom])))
        *query-params (cursor *node [:props :query-params])
        query-param-forms
        (->> (cursor-vec *node [:props :query-params])
             (map-indexed
               (fn [index *query-param]
                 ^{:key index} [form-group-key-val-pair
                                *query-params
                                index
                                *query-param])))
        *body (cursor *node [:props :body])]
    [:form
     [:div.help-block
      [commons/link-docs "http-request"]]

     [:div.form-group
      [:label "Import "]
      [:p
       [commons/button-swagger
        {:on-click (fn [e] (dialogs/show! [dialog-swagger-import *node]))}
        "Import from Swagger"]]]

     [form-group-bindable {:name "Name"} (cursor *node [:name])]
     [form-group-bindable
      {:name "Url"
       :spec ::http-node/url}
      (cursor *node [:props :url])]

     [:label "Query Params "]
     query-param-forms
     [button-add-new-key-val *query-params]

     [:div.form-group
      [:label "Method"]
      [:select.form-control
       {:on-change #(reset! *http-method (-> % .-target .-value))
        :value     (if (empty? @*http-method) "GET" @*http-method)}
       [:option {:value "GET"} "GET"]
       [:option {:value "POST"} "POST"]
       [:option {:value "PUT"} "PUT"]
       [:option {:value "DELETE"} "DELETE"]
       [:option {:value "PATCH"} "PATCH"]
       [:option {:value "OPTIONS"} "OPTIONS"]
       [:option {:value "HEAD"} "HEAD"]
       [:option {:value "CONNECT"} "CONNECT"]]]

     (let [content-type (find-content-type (:props @*node))]
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
