(ns apibot.grexec.extract-body-node
  "Equivalent to eval-node but uses JavaScript instead of ClojureScript."
  (:require
    #?(:cljs [cljs.spec.alpha :as s] :clj [clojure.spec.alpha :as s])
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.eval :as eval]
    [promesa.core :as p]
    [clojure.string :as string]))

(def doctext
  (string/join "\n"
               ["/**"
                " * An extract-body function takes the last HTTP"
                " * response's body as input and returns anything."
                " * The result of this function will be appended to"
                " * the scope under the name specified by the"
                " * 'Property Name' above"
                " */"
                "(body) => {"
                "  // Replace this with your own logic."
                "  return body['user']['id'];"
                "}"]))

;; ---- Spec ----

(s/def ::fn eval/is-js-function?)
(s/def ::name (s/and string? (complement empty?)))

(s/def ::props
  (s/keys :req [::fn ::name]))

;; ---- API ----

(defn execute
  [node scope]
  (let [key-name (-> node :props :name)
        func (-> node :props :fn eval/evaluate-js-function)
        body (get-in scope [:apibot|http-response :body])
        value (func body)]
    (p/promise (assoc scope key-name value))))

(def graph
  (map->NativeGraph
    {:id       "extract-body"
     :name     "Extract Body"
     :desc     "Extracts the value with the given path"
     :execfunc execute
     :default-props {:fn doctext}
     :spec     nil}))
