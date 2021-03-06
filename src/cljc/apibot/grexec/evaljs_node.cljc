(ns apibot.grexec.evaljs-node
  "Equivalent to eval-node but uses JavaScript instead of ClojureScript."
  (:require
    [apibot.graphs :refer [map->NativeGraph]]
    [apibot.grexec.executors :as executors]
    [apibot.grexec.eval :as eval]
    [promesa.core :as p]
    [clojure.string :as string]))

(def doctext
  (string/join "\n"
               ["/**"
                " * An evaljs function takes the scope as input"
                " * and returns the new scope."
                " * "
                " * @return [Scope] the new scope."
                " */"
                "(scope) => {"
                "  // Replace this with your own logic."
                "  scope.email = 'random@email.tk';"
                "  return scope;"
                "}"]))

;; ---- API ----

(defn execute
  [node scope]
  (let [func (-> node :props :fn eval/evaluate-js-function)]
    (let [result (func scope)]
      (p/promise
        (if (map? result)
          result
          (assoc scope
            :apibot|error true
            :apibot|js-error
            (str "The result of an EvalJS node must always be a non-null javascript object but "
                 "instead returned '" result "'.")))))))



(def graph
  (map->NativeGraph
    {:id       "evaljs"
     :name     "Eval JavaScript"
     :desc     "Evaluates JavaScript"
     :execfunc (executors/wrap-with-try-catch execute executors/on-js-error)
     :default-props {:fn doctext}
     :spec     nil}))
