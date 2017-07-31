(ns apibot.views.tree
  "A view that renders a Tree-kinda data structure."
  (:require
    [reagent.core :refer [atom]]))

(declare tree)

(defn header [text expanded-ratom]
  ""
  (let [class (if @expanded-ratom
                "glyphicon-triangle-bottom"
                "glyphicon-triangle-right")
        text-str (if (keyword? text)
                   (name text)
                   (str text))]
    [:div.tree-view-item
     [:button.btn.btn-link
      {:on-click (fn [e] (swap! expanded-ratom not))
       :style    {:outline "none" :padding-left "0px"}}
      [:span.glyphicon {:class class}]
      [:samp text-str]]]))

(defn children
  [obj expanded-ratom]
  [:div.tree-view-children
   (if @expanded-ratom
     [tree obj])])

(defn tree
  "WARN: as opposed to other views, this does not take a ratom as argument"
  [tree-obj]
  (let [indentation-size "10px"]
    [:div
     (cond
       (nil? tree-obj)
       [:samp "no contents"]

       (or (string? tree-obj) (number? tree-obj) (boolean? tree-obj)
           (instance? js/Error tree-obj)
           (fn? tree-obj))
       [:samp {:style {:white-space "pre"}} (str tree-obj)]

       (and (coll? tree-obj) (empty? tree-obj))
       [:samp "empty collection"]

       (map? tree-obj)
       (for [[k v] tree-obj]
         (let [expanded (atom false)]
           [:div.tree-view {:key k :style {:padding-left indentation-size}}
            [header k expanded]
            [children v expanded]]))

       (coll? tree-obj)
       (for [[elem idx] (map vector tree-obj (range))]
         (let [expanded (atom false)
               key (str "[" idx "]")]
           [:div.tree-view {:key key :style {:padding-left indentation-size}}
            [header key expanded]
            [children elem expanded]]))

       :else
       [:samp "Unable to render unknown object: " tree-obj])]))
