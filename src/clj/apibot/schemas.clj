(ns apibot.schemas
  (:require [schema.core :as s]))

(s/defschema User
  {(s/optional-key :id) s/Str
   :user-id             s/Str
   :email               s/Str
   :picture             s/Str})

(s/defschema Edge
  {:source s/Str
   :target s/Str
   :id     s/Str})

(s/defschema Node
  {:id       s/Str
   :graph-id s/Str
   :name     s/Str
   :position {:x s/Num :y s/Num}
   :props    {s/Keyword s/Any}
   :type     s/Str})

(s/defschema Graph
  {:id                       s/Str
   (s/optional-key :user-id) (s/maybe s/Str)
   :desc                     s/Str
   :edges                    [Edge]
   :executable               s/Bool
   :nodes                    [Node]
   :name                     s/Str})

(s/defschema Scope
  {s/Keyword s/Any})

(s/defschema ExecutionStep
  "Represents a step in an execution"
  {:node Node
   :id s/Str
   :scope Scope
   :start-time s/Num
   :end-time s/Num})

(s/defschema Execution
  "An execution represents the result of executing a graph."
  {:id   s/Str
   (s/optional-key :user-id) (s/maybe s/Str)
   :graph-id s/Str
   :name s/Str
   :created-at s/Num
   :history [ExecutionStep]})

(s/defschema LightExecution
  "A minified version of Execution. For full history access see Execution/ExecutionStep."
  {:id s/Str
   :graph-id s/Str
   :name s/Str
   :created-at s/Num
   :result {:failed s/Bool}})
