(ns hiccdown.hiccup
  (:require [clojure.string :as str]
            [hiccdown.error :as error]
            [hiccdown.ir :as ir]))

(declare node->hiccup hiccup-node->ir)

(defn- escape-html [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")
      (str/replace "'" "&#39;")))

(defn- children->hiccup [children opts]
  (mapv #(node->hiccup % opts) children))

(defn- html-policy [node opts]
  (case (:html opts)
    :raw (:text node)
    :escape (escape-html (:text node))
    (error/unsupported! :ir->hiccup :raw-html-disabled
                        "Raw HTML cannot be emitted as Hiccup unless {:html :raw} or {:html :escape} is supplied"
                        {:node node})))

(defn- table-section->hiccup [node tag opts]
  (into [tag] (children->hiccup (:children node) opts)))

(defn node->hiccup [node opts]
  (case (:type node)
    :document (into [:div] (children->hiccup (:children node) opts))
    :paragraph (into [:p] (children->hiccup (:children node) opts))
    :heading (into [(keyword (str "h" (get-in node [:attrs :level])))] (children->hiccup (:children node) opts))
    :text (:text node)
    :emphasis (into [:em] (children->hiccup (:children node) opts))
    :strong (into [:strong] (children->hiccup (:children node) opts))
    :strikethrough (into [:del] (children->hiccup (:children node) opts))
    :inline-code [:code (:text node)]
    :code-block [:pre [:code (cond-> {}
                               (get-in node [:attrs :language]) (assoc :class (str "language-" (get-in node [:attrs :language]))))
                       (:text node)]]
    :blockquote (into [:blockquote] (children->hiccup (:children node) opts))
    :ordered-list (into [:ol (cond-> {}
                               (not= 1 (or (get-in node [:attrs :start]) 1)) (assoc :start (get-in node [:attrs :start])))]
                        (children->hiccup (:children node) opts))
    :unordered-list (into [:ul] (children->hiccup (:children node) opts))
    :list-item (into [:li] (children->hiccup (:children node) opts))
    :link (into [:a (cond-> {:href (get-in node [:attrs :href])}
                      (get-in node [:attrs :title]) (assoc :title (get-in node [:attrs :title])))]
                (children->hiccup (:children node) opts))
    :image [:img (cond-> {:src (get-in node [:attrs :src])
                          :alt (or (get-in node [:attrs :alt]) "")}
                   (get-in node [:attrs :title]) (assoc :title (get-in node [:attrs :title])))]
    :hard-break [:br]
    :soft-break "\n"
    :thematic-break [:hr]
    :html-block (html-policy node opts)
    :html-inline (html-policy node opts)
    :table (into [:table] (children->hiccup (:children node) opts))
    :table-head (table-section->hiccup node :thead opts)
    :table-body (table-section->hiccup node :tbody opts)
    :table-row (into [:tr] (children->hiccup (:children node) opts))
    :table-cell (into [(if (get-in node [:attrs :header?]) :th :td)] (children->hiccup (:children node) opts))
    (error/render! :ir->hiccup :unsupported-node "Unsupported IR node" {:node node})))

(defn ir->hiccup
  ([ir-node]
   (ir->hiccup ir-node {}))
  ([ir-node opts]
   (ir/validate-node ir-node)
   (node->hiccup ir-node opts)))

(defn- attrs? [x] (map? x))

(defn- split-hiccup [v]
  (when-not (and (vector? v) (keyword? (first v)))
    (error/invalid-hiccup! :hiccup->ir :invalid-vector "Hiccup node must be a vector beginning with a keyword" {:node v}))
  (let [[tag & xs] v
        attrs (if (attrs? (first xs)) (first xs) {})
        children (if (attrs? (first xs)) (rest xs) xs)]
    [tag attrs children]))

(defn- inline-children [children opts]
  (mapv #(hiccup-node->ir % opts) children))

(defn- text-content [node]
  (cond
    (string? node) node
    (number? node) (str node)
    (vector? node) (let [[_ _ children] (split-hiccup node)]
                     (apply str (map text-content children)))
    :else ""))

(defn hiccup-node->ir [node opts]
  (cond
    (string? node) (ir/text node)
    (number? node) (ir/text node)
    (nil? node) (ir/text "")
    (vector? node)
    (let [[tag attrs children] (split-hiccup node)]
      (case tag
        :div (ir/node :document {} (inline-children children opts))
        :article (ir/node :document {} (inline-children children opts))
        :p (ir/node :paragraph {} (inline-children children opts))
        :h1 (ir/node :heading {:level 1} (inline-children children opts))
        :h2 (ir/node :heading {:level 2} (inline-children children opts))
        :h3 (ir/node :heading {:level 3} (inline-children children opts))
        :h4 (ir/node :heading {:level 4} (inline-children children opts))
        :h5 (ir/node :heading {:level 5} (inline-children children opts))
        :h6 (ir/node :heading {:level 6} (inline-children children opts))
        :em (ir/node :emphasis {} (inline-children children opts))
        :i (ir/node :emphasis {} (inline-children children opts))
        :strong (ir/node :strong {} (inline-children children opts))
        :b (ir/node :strong {} (inline-children children opts))
        :del (ir/node :strikethrough {} (inline-children children opts))
        :s (ir/node :strikethrough {} (inline-children children opts))
        :code (if (:block? opts)
                {:type :code-block :attrs {:info nil :language nil} :text (text-content node)}
                {:type :inline-code :text (text-content node)})
        :pre (let [code-child (first (filter #(and (vector? %) (= :code (first %))) children))
                   code-text (if code-child (text-content code-child) (apply str (map text-content children)))]
               {:type :code-block :attrs {:info nil :language nil} :text code-text})
        :blockquote (ir/node :blockquote {} (inline-children children opts))
        :ol (ir/node :ordered-list {:start (or (:start attrs) 1)} (inline-children children opts))
        :ul (ir/node :unordered-list {} (inline-children children opts))
        :li (ir/node :list-item {} (inline-children children opts))
        :a (ir/node :link {:href (:href attrs) :title (:title attrs)} (inline-children children opts))
        :img {:type :image :attrs {:src (:src attrs) :alt (:alt attrs) :title (:title attrs)}}
        :br {:type :hard-break}
        :hr (ir/node :thematic-break {} [])
        :table (ir/node :table {} (inline-children children opts))
        :thead (ir/node :table-head {} (inline-children children opts))
        :tbody (ir/node :table-body {} (inline-children children opts))
        :tr (ir/node :table-row {} (inline-children children opts))
        :th (ir/node :table-cell {:header? true} (inline-children children opts))
        :td (ir/node :table-cell {:header? false} (inline-children children opts))
        (error/unsupported! :hiccup->ir :unsupported-tag "Unsupported Hiccup tag" {:tag tag :node node})))
    :else
    (error/invalid-hiccup! :hiccup->ir :unsupported-value "Unsupported Hiccup value" {:node node})))

(defn hiccup->ir
  ([hiccup]
   (hiccup->ir hiccup {}))
  ([hiccup opts]
   (let [node (hiccup-node->ir hiccup opts)
         doc (if (= :document (:type node)) node (ir/node :document {} [node]))]
     (ir/validate-node doc))))
