(ns hiccdown.ir
  (:require [hiccdown.error :as error]))

(def container-types
  #{:document :paragraph :heading :emphasis :strong :strikethrough
    :blockquote :ordered-list :unordered-list :list-item :link
    :table :table-head :table-body :table-row :table-cell})

(def text-types
  #{:text :inline-code :code-block :html-block :html-inline})

(def leaf-types
  #{:image :hard-break :soft-break :thematic-break})

(defn node
  ([type]
   (node type {} []))
  ([type attrs children]
   {:type type :attrs (or attrs {}) :children (vec children)}))

(defn text [s]
  {:type :text :text (str s)})

(defn leaf [type]
  {:type type})

(defn text-node [type s]
  {:type type :text (str s)})

(defn validate-node
  ([ir-node]
   (validate-node ir-node []))
  ([ir-node path]
   (when-not (map? ir-node)
     (error/invalid-ir! :ir :node-not-map "IR node must be a map" {:path path}))
   (let [type (:type ir-node)]
     (when-not (keyword? type)
       (error/invalid-ir! :ir :missing-type "IR node requires keyword :type" {:path path :node ir-node}))
     (cond
       (container-types type)
       (do
         (when (and (contains? ir-node :attrs) (not (map? (:attrs ir-node))))
           (error/invalid-ir! :ir :attrs-not-map "IR :attrs must be a map" {:path path :node ir-node}))
         (when-not (vector? (:children ir-node))
           (error/invalid-ir! :ir :children-not-vector "Container IR node requires vector :children" {:path path :node ir-node}))
         (run! (fn [[idx child]] (validate-node child (conj path idx)))
               (map-indexed vector (:children ir-node))))

       (text-types type)
       (when-not (string? (:text ir-node))
         (error/invalid-ir! :ir :text-not-string "Text-like IR node requires string :text" {:path path :node ir-node}))

       (leaf-types type)
       true

       :else
       (error/invalid-ir! :ir :unknown-type "Unknown IR node type" {:path path :node ir-node}))
     ir-node)))
