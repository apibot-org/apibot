(ns apibot.grexec.http-node
  (:require
    [apibot.el :as el]
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.http :refer [http-request!]]
    [apibot.util :as util :refer [key-val-headers->map keywordize-keys]]
    [cats.monad.either :refer [branch]]
    [cljs.spec.alpha :as s]
    [clojure.string :refer [lower-case includes?]]
    [httpurr.client :as http]
    [httpurr.client.xhr :as xhr]
    [promesa.core :as p]))

;; ---- Specs ----

(s/def ::http-method #{"GET" "POST" "PUT" "DELETE" "PATCH" "OPTIONS"})
(s/def ::url (complement empty?))
(s/def ::body string?)
(s/def ::header (fn [{:keys [key val]}]
                  (and (not (empty? key))
                       (not (empty? val)))))
(s/def ::headers (s/* ::header))
(s/def ::props
  (s/keys :req [::http-method
                ::url
                ::body
                ::headers]))

;; ---- API ----

(defn execute
  "Performs an HTTP request and appends the scope with the
  :apibot.http-request and :apibot.http-response keys."
  [node scope]
  (let [; clear the apibot.http-(request|response) keys from the scope. This way if there is
        ; an error and the request is not executed, it is obvious to the user which request/response
        ; we are talking about.
        scope (dissoc scope :apibot.http-request :apibot.http-response)
        request (-> (:props node)
                    (update :headers key-val-headers->map)
                    (update :query-params key-val-headers->map))
        either-request (el/render request scope)]
    (branch either-request
            (fn [error]
              (assoc scope
                :apibot.error true
                :apibot.el-error error))
            (fn [rendered-request]
              (-> (http-request! rendered-request {:proxy true})
                  (p/then
                    (fn [result]
                      ;; XXX apparently Httpur responds with status 0 on IO exceptions.
                      ;; it looks safe to make this check, but this should be revised
                      ;; in the future.
                      (if (= 0 (:status result))
                        (assoc scope
                          :apibot.http-request rendered-request
                          :apibot.error true
                          :apibot.http-error
                          (str "The request could not be executed, likely due to "
                               "connection problems. Please check your internet \n"
                               "connection and try again."))
                        (assoc scope
                          :apibot.http-request rendered-request
                          :apibot.http-response result))))

                  (p/catch
                    (fn [error]
                      (assoc scope
                        :apibot.error true
                        :apibot.http-error error))))))))


(def graph
  (map->NativeGraph
    {:id       "http-request"
     :name     "HTTP Request"
     :desc     "Makes an HTTP Request"
     :execfunc execute
     :spec     nil}))
