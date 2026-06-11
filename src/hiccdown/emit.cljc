(ns hiccdown.emit
  (:require [clojure.string :as str]
            [hiccdown.error :as error]
            [hiccdown.ir :as ir]
            [hiccdown.markdown :as md]))

(declare render-inline render-block)

(defn- inline-text [nodes]
  (apply str (map render-inline nodes)))

(defn- render-inline [node]
  (case (:type node)
    :text (md/escape-md (:text node))
    :emphasis (str "*" (inline-text (:children node)) "*")
    :strong (str "**" (inline-text (:children node)) "**")
    :strikethrough (str "~~" (inline-text (:children node)) "~~")
    :inline-code (str "`" (str/replace (:text node) "`" "\\`") "`")
    :link (let [{:keys [href title]} (:attrs node)]
            (str "[" (inline-text (:children node)) "]("
                 href
                 (when title (str " \"" title "\""))
                 ")"))
    :image (let [{:keys [src alt title]} (:attrs node)]
             (str "![" (md/escape-md (or alt "")) "]("
                  src
                  (when title (str " \"" title "\""))
                  ")"))
    :hard-break "  \n"
    :soft-break "\n"
    :html-inline (:text node)
    (error/render! :ir->markdown :unsupported-inline "Unsupported inline IR node" {:node node})))

(defn- indent-lines [prefix s]
  (->> (str/split-lines s)
       (map #(str prefix %))
       (str/join "\n")))

(defn- list-item-md [ordered? idx node]
  (let [marker (if ordered? (str idx ". ") "- ")
        body (str/trimr (apply str (map render-block (:children node))))
        lines (str/split body #"\n")]
    (str marker (first lines)
         (when-let [rest-lines (seq (rest lines))]
           (str "\n" (str/join "\n" (map #(str (apply str (repeat (count marker) " ")) %) rest-lines))))
         "\n")))

(defn- table-row [cells header?]
  (str "| "
       (str/join " | "
                 (map (fn [cell]
                        (let [text (inline-text (:children cell))]
                          (if header? (md/escape-table-cell text) (md/escape-table-cell text))))
                      cells))
       " |\n"))

(defn- render-table [node]
  (let [[head body] (:children node)
        head-row (first (:children head))
        headers (:children head-row)
        rows (mapcat :children [body])]
    (str (table-row headers true)
         "| " (str/join " | " (repeat (count headers) "---")) " |\n"
         (apply str (map #(table-row (:children %) false) rows)))))

(defn- render-block [node]
  (case (:type node)
    :paragraph (str (inline-text (:children node)) "\n\n")
    :heading (str (apply str (repeat (get-in node [:attrs :level]) "#"))
                  " " (inline-text (:children node)) "\n\n")
    :thematic-break "---\n\n"
    :blockquote (str (indent-lines "> " (str/trimr (apply str (map render-block (:children node))))) "\n\n")
    :ordered-list (let [start (or (get-in node [:attrs :start]) 1)]
                    (str (apply str (map-indexed #(list-item-md true (+ start %1) %2) (:children node))) "\n"))
    :unordered-list (str (apply str (map-indexed #(list-item-md false (inc %1) %2) (:children node))) "\n")
    :code-block (let [info (or (get-in node [:attrs :info]) "")]
                  (str "```" info "\n" (:text node) (when-not (str/ends-with? (:text node) "\n") "\n") "```\n\n"))
    :html-block (str (:text node) (when-not (str/ends-with? (:text node) "\n") "\n") "\n")
    :table (str (render-table node) "\n")
    (error/render! :ir->markdown :unsupported-block "Unsupported block IR node" {:node node})))

(defn ir->md
  ([ir-node]
   (ir->md ir-node {}))
  ([ir-node _opts]
   (ir/validate-node ir-node)
   (when-not (= :document (:type ir-node))
     (error/invalid-ir! :ir->markdown :expected-document "IR root must be :document" {:node ir-node}))
   (str/trimr (apply str (map render-block (:children ir-node))))))
