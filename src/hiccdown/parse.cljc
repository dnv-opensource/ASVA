(ns hiccdown.parse
  (:require [clojure.string :as str]
            [hiccdown.error :as error]
            [hiccdown.ir :as ir]
            [hiccdown.markdown :as md]))

(declare parse-blocks parse-inlines)

(defn- line-at [lines idx] (nth lines idx ""))

(defn- fence-start [line]
  (when-let [[_ indent fence info] (re-matches #"^( {0,3})(`{3,}|~{3,})[ \t]*(.*)$" line)]
    {:indent (count indent)
     :char (first fence)
     :size (count fence)
     :info (str/trim info)}))

(defn- fence-end? [{:keys [char size]} line]
  (boolean
   (when-let [[_ fence] (re-matches #"^ {0,3}(`{3,}|~{3,})[ \t]*$" line)]
     (and (= (first fence) char) (>= (count fence) size)))))

(defn- heading-line [line]
  (when-let [[_ marks text] (re-matches #"^ {0,3}(#{1,6})(?:[ \t]+|$)(.*)$" line)]
    (let [body (str/trim (str/replace text #"[ \t]+#+[ \t]*$" ""))]
      {:level (count marks) :text body})))

(defn- thematic-break? [line]
  (boolean
   (re-matches #"^ {0,3}((\*[ \t]*){3,}|(-[ \t]*){3,}|(_[ \t]*){3,})$" line)))

(defn- blockquote-line [line]
  (when-let [[_ body] (re-matches #"^ {0,3}> ?(.*)$" line)]
    body))

(defn- list-marker [line]
  (when-let [[_ indent marker rest] (re-matches #"^( *)([*+-]|\d+[.)])[ \t]+(.*)$" line)]
    (let [ordered? (boolean (re-matches #"\d+[.)]" marker))]
      {:indent (count indent)
       :ordered? ordered?
       :start (when ordered? #?(:clj (Long/parseLong (subs marker 0 (dec (count marker))))
                                :cljs (js/parseInt (subs marker 0 (dec (count marker))) 10)))
       :rest rest})))

(defn- split-table-row [line]
  (let [trimmed (str/trim line)
        row (cond-> trimmed
              (str/starts-with? trimmed "|") (subs 1)
              (str/ends-with? trimmed "|") (subs 0 (dec (count trimmed))))]
    (mapv str/trim (str/split row #"(?<!\\)\|"))))

(defn- table-separator? [line]
  (let [cells (split-table-row line)]
    (and (>= (count cells) 2)
         (every? #(re-matches #":?-{3,}:?" %) cells))))

(defn- html-block-line? [line]
  (boolean
   (re-find #"^ {0,3}(<!--|</?[A-Za-z][A-Za-z0-9-]*(?:\s|>|/>)|<![A-Z]+|<\?)" line)))

(defn- parse-code-fence [lines idx]
  (let [start (fence-start (line-at lines idx))
        from (inc idx)]
    (loop [i from acc []]
      (cond
        (>= i (count lines))
        (error/parse! :markdown->ir :unclosed-code-fence "Unclosed fenced code block" {:line (inc idx) :column 1})

        (fence-end? start (line-at lines i))
        (let [info (:info start)
              language (first (str/split info #"\s+"))]
          [{:type :code-block
            :attrs {:info (when-not (str/blank? info) info)
                    :language (when-not (str/blank? language) language)}
            :text (str (str/join "\n" acc) "\n")}
           (inc i)])

        :else
        (recur (inc i) (conj acc (md/trim-indent (:indent start) (line-at lines i))))))))

(defn- parse-indented-code [lines idx]
  (loop [i idx acc []]
    (if (and (< i (count lines))
             (or (md/blank? (line-at lines i))
                 (>= (md/leading-spaces (line-at lines i)) 4)))
      (recur (inc i) (conj acc (if (md/blank? (line-at lines i))
                                 ""
                                 (md/trim-indent 4 (line-at lines i)))))
      [{:type :code-block :attrs {:info nil :language nil} :text (str (str/join "\n" acc) "\n")} i])))

(defn- parse-blockquote [lines idx]
  (loop [i idx acc []]
    (if-let [body (and (< i (count lines)) (blockquote-line (line-at lines i)))]
      (recur (inc i) (conj acc body))
      [(ir/node :blockquote {} (parse-blocks acc 0 0)) i])))

(defn- collect-list-item-continuation [lines start base-indent]
  (loop [i start acc []]
    (cond
      (>= i (count lines)) [acc i]
      (md/blank? (line-at lines i)) (recur (inc i) (conj acc ""))
      (and (list-marker (line-at lines i))
           (<= (:indent (list-marker (line-at lines i))) base-indent)) [acc i]
      (< (md/leading-spaces (line-at lines i)) (inc base-indent)) [acc i]
      :else (recur (inc i) (conj acc (md/trim-indent (inc base-indent) (line-at lines i)))))))

(defn- parse-list [lines idx]
  (let [first-marker (list-marker (line-at lines idx))
        ordered? (:ordered? first-marker)
        base-indent (:indent first-marker)]
    (loop [i idx items []]
      (if-let [marker (and (< i (count lines)) (list-marker (line-at lines i)))]
        (if (and (= ordered? (:ordered? marker)) (= base-indent (:indent marker)))
          (let [[continuation next-i] (collect-list-item-continuation lines (inc i) base-indent)
                item-lines (into [(:rest marker)] continuation)
                children (parse-blocks item-lines 0 0)]
            (recur next-i (conj items (ir/node :list-item {} children))))
          [(ir/node (if ordered? :ordered-list :unordered-list)
                    (if ordered? {:start (:start first-marker)} {})
                    items)
           i])
        [(ir/node (if ordered? :ordered-list :unordered-list)
                  (if ordered? {:start (:start first-marker)} {})
                  items)
         i]))))

(defn- parse-table [lines idx]
  (let [header (split-table-row (line-at lines idx))
        separator (split-table-row (line-at lines (inc idx)))]
    (when-not (= (count header) (count separator))
      (error/parse! :markdown->ir :invalid-table-separator "Malformed Markdown table"
                    {:line (+ idx 2) :column 1}))
    (loop [i (+ idx 2) rows []]
      (if (and (< i (count lines))
               (not (md/blank? (line-at lines i)))
               (str/includes? (line-at lines i) "|"))
        (let [cells (split-table-row (line-at lines i))]
          (when-not (= (count cells) (count header))
            (error/parse! :markdown->ir :invalid-table-row "Malformed Markdown table row"
                          {:line (inc i) :column 1}))
          (recur (inc i) (conj rows cells)))
        [(ir/node :table {}
                  [(ir/node :table-head {}
                            [(ir/node :table-row {}
                                      (mapv #(ir/node :table-cell {:header? true} (parse-inlines %)) header))])
                   (ir/node :table-body {}
                            (mapv (fn [row]
                                    (ir/node :table-row {}
                                             (mapv #(ir/node :table-cell {:header? false} (parse-inlines %)) row)))
                                  rows))])
         i]))))

(defn- parse-html-block [lines idx]
  (loop [i idx acc []]
    (if (and (< i (count lines)) (not (md/blank? (line-at lines i))))
      (recur (inc i) (conj acc (line-at lines i)))
      [{:type :html-block :text (str (str/join "\n" acc) "\n")} i])))

(defn- paragraph-break? [lines i]
  (let [line (line-at lines i)]
    (or (md/blank? line)
        (heading-line line)
        (thematic-break? line)
        (fence-start line)
        (blockquote-line line)
        (list-marker line)
        (html-block-line? line)
        (and (< (inc i) (count lines))
             (str/includes? line "|")
             (table-separator? (line-at lines (inc i)))))))

(defn- parse-paragraph [lines idx]
  (loop [i idx acc []]
    (if (or (>= i (count lines))
            (and (> i idx) (paragraph-break? lines i)))
      [(ir/node :paragraph {} (parse-inlines (str/join "\n" acc))) i]
      (recur (inc i) (conj acc (str/trim (line-at lines i)))))))

(defn parse-blocks
  ([lines idx min-indent]
   (loop [i idx nodes []]
     (if (>= i (count lines))
       nodes
       (let [line (line-at lines i)]
         (cond
           (md/blank? line) (recur (inc i) nodes)

           (fence-start line)
           (let [[node next-i] (parse-code-fence lines i)]
             (recur next-i (conj nodes node)))

           (>= (md/leading-spaces line) 4)
           (let [[node next-i] (parse-indented-code lines i)]
             (recur next-i (conj nodes node)))

           (heading-line line)
           (let [{:keys [level text]} (heading-line line)]
             (recur (inc i) (conj nodes (ir/node :heading {:level level} (parse-inlines text)))))

           (thematic-break? line)
           (recur (inc i) (conj nodes (ir/node :thematic-break {} [])))

           (blockquote-line line)
           (let [[node next-i] (parse-blockquote lines i)]
             (recur next-i (conj nodes node)))

           (list-marker line)
           (let [[node next-i] (parse-list lines i)]
             (recur next-i (conj nodes node)))

           (and (< (inc i) (count lines))
                (str/includes? line "|")
                (table-separator? (line-at lines (inc i))))
           (let [[node next-i] (parse-table lines i)]
             (recur next-i (conj nodes node)))

           (html-block-line? line)
           (let [[node next-i] (parse-html-block lines i)]
             (recur next-i (conj nodes node)))

           :else
           (let [[node next-i] (parse-paragraph lines i)]
             (recur next-i (conj nodes node)))))))))

(defn- text-buffer [acc s]
  (if (str/blank? s) acc (conj acc (ir/text s))))

(defn- find-close [s open close from]
  (let [n (count s)]
    (loop [i from]
      (cond
        (>= i n) nil
        (= \\ (nth s i)) (recur (+ i 2))
        (and (<= (+ i (count close)) n)
             (= close (subs s i (+ i (count close))))) i
        :else (recur (inc i))))))

(defn- parse-link-or-image [s i image?]
  (let [label-start (+ i (if image? 2 1))
        label-end (find-close s "[" "]" label-start)]
    (when label-end
      (let [label-end ^number label-end
            dest-end (find-close s "(" ")" (+ label-end 2))]
        (when (and (< (inc label-end) (count s)) (= \( (nth s (inc label-end))) dest-end)
          (let [dest-end ^number dest-end
                label (subs s label-start label-end)
                dest (str/trim (subs s (+ label-end 2) dest-end))
                [_ href title] (or (re-matches #"([^ \t]+)[ \t]+\"([^\"]*)\"" dest)
                                   (re-matches #"([^ \t]+)[ \t]+'([^']*)'" dest)
                                   [nil dest nil])]
            {:end (inc dest-end)
             :node (if image?
                     {:type :image :attrs {:src href :alt label :title title}}
                     (ir/node :link {:href href :title title} (parse-inlines label)))}))))))

(defn- parse-autolink [s i]
  (when-let [end (find-close s "<" ">" (inc i))]
    (let [end ^number end
          body (subs s (inc i) end)]
      (cond
        (re-matches #"(?i)[a-z][a-z0-9+.-]{1,31}:[^<>\s]*" body)
        {:end (inc end)
         :node (ir/node :link {:href body :title nil} [(ir/text body)])}

        (re-matches #"(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}" body)
        {:end (inc end)
         :node (ir/node :link {:href (str "mailto:" body) :title nil} [(ir/text body)])}

        (md/html-like? (str "<" body ">"))
        {:end (inc end) :node {:type :html-inline :text (str "<" body ">")}}

        :else nil))))

(defn- parse-delimited [s i delim type]
  (when-let [end (find-close s delim delim (+ i (count delim)))]
    (let [end ^number end]
      {:end (+ end (count delim))
       :node (ir/node type {} (parse-inlines (subs s (+ i (count delim)) end)))})))

(defn parse-inlines [s]
  (let [n (count s)]
    (loop [i 0 buf "" acc []]
      (if (>= i n)
        (text-buffer acc buf)
        (let [ch (nth s i)
              two (when (< (inc i) n) (subs s i (+ i 2)))]
          (cond
            (and (= ch \\) (< (inc i) n) (contains? md/escapable (nth s (inc i))))
            (recur (+ i 2) (str buf (nth s (inc i))) acc)

            (and (= ch \space) (< (inc i) n) (= \space (nth s (inc i)))
                 (< (+ i 2) n) (= \newline (nth s (+ i 2))))
            (recur (+ i 3) "" (conj (text-buffer acc buf) {:type :hard-break}))

            (= ch \newline)
            (recur (inc i) "" (conj (text-buffer acc buf) {:type :soft-break}))

            (= ch \`)
            (if-let [end (find-close s "`" "`" (inc i))]
              (let [end ^number end]
                (recur (inc end) "" (conj (text-buffer acc buf)
                                          {:type :inline-code :text (subs s (inc i) end)})))
              (recur (inc i) (str buf ch) acc))

            (= two "![")
            (if-let [{:keys [end node]} (parse-link-or-image s i true)]
              (recur end "" (conj (text-buffer acc buf) node))
              (recur (inc i) (str buf ch) acc))

            (= ch \[)
            (if-let [{:keys [end node]} (parse-link-or-image s i false)]
              (recur end "" (conj (text-buffer acc buf) node))
              (recur (inc i) (str buf ch) acc))

            (= ch \<)
            (if-let [{:keys [end node]} (parse-autolink s i)]
              (recur end "" (conj (text-buffer acc buf) node))
              (recur (inc i) (str buf ch) acc))

            (or (= two "**") (= two "__"))
            (if-let [{:keys [end node]} (parse-delimited s i two :strong)]
              (recur end "" (conj (text-buffer acc buf) node))
              (recur (inc i) (str buf ch) acc))

            (= two "~~")
            (if-let [{:keys [end node]} (parse-delimited s i "~~" :strikethrough)]
              (recur end "" (conj (text-buffer acc buf) node))
              (recur (inc i) (str buf ch) acc))

            (or (= ch \*) (= ch \_))
            (if-let [{:keys [end node]} (parse-delimited s i (str ch) :emphasis)]
              (recur end "" (conj (text-buffer acc buf) node))
              (recur (inc i) (str buf ch) acc))

            :else
            (recur (inc i) (str buf ch) acc)))))))

(defn md->ir
  ([markdown-string]
   (md->ir markdown-string {}))
  ([markdown-string _opts]
   (when-not (string? markdown-string)
     (error/parse! :markdown->ir :input-not-string "Markdown input must be a string"))
   (let [doc (ir/node :document {} (parse-blocks (md/split-lines markdown-string) 0 0))]
     (ir/validate-node doc))))
