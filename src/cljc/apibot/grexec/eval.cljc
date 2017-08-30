(ns apibot.grexec.eval
  "A namespace for evaluating arbitrary code in a safe way"
  (:require
    #?(:clj [apibot.config :as config])
    [apibot.coll :as coll]
    [apibot.http :as http]))


(defn is-js-function?
  "Returns true if the given string represents a javascript function."
  [string]
  #?(:clj (string? string) ;; TODO: add better checks
     :cljs (try
              (fn? (js/eval string))
              (catch js/Object e
                false))))

#?(:clj
    (defn- lambda-invoke [js-code args]
      (http/http-request!
        {:http-method :post
         :url (str config/aws-lambda-api-url "/eval")
         :headers {:x-api-key config/aws-lambda-api-key
                   :content-type "application/json"}
         :body {:funcString js-code
                :args args}})))

(defn evaluate-js-function
  "Given a javascript string representing a function,
  returns a clojurized function."
  [js-code]
  #?(:clj
      (fn [& args]
        (lambda-invoke js-code args))
     :cljs
      (let [fun (js/eval js-code)]
        (fn [& args]
          (let [js-args (map clj->js args)]
            (-> (apply fun js-args)
                (js->clj :keywordize-keys true)))))))
