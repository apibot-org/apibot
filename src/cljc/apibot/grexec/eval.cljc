(ns apibot.grexec.eval
  "A namespace for evaluating arbitrary code in a safe way"
  (:require
    [apibot.coll :as coll]))


(defn is-js-function?
  "Returns true if the given string represents a javascript function."
  [string]
  #?(:clj (string? string) ;; TODO: add better checks
     :cljs (try
              (fn? (js/eval string))
              (catch js/Object e
                false))))

(defn lambda-invoke [js-code args]
  (throw (ex-info "Unimplemented Function" {})))

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
