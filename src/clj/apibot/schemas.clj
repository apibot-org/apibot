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
   :position {:x Double :y Double}
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
