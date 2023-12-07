(ns hiccdown.core
  (:require
   [clojure.string :as str]
   [instaparse.core :as insta]
   [instaparse.failure :as fail]))

(defn ->params
  "Normalize arguments to always have the form [props children] like
   hiccup elements."
  [args]
  (cond-> args
    (-> args first map? not) (conj nil)))

(def ^:private markdown-parser
  (insta/parser
   "markdown = element+
    <element> = headings | hr | img | link | blocks | p | br
    
    (* ATX and Setext heading support *)
    <headings> = h1 / h2 / h3 / h4 / h5 / h6
    h1 = <sol> <'#' space> words <eol> | words <eol #'={2,}' eol>
    h2 = <sol> <'##' space> words <eol> | words <eol #'-{2,}' eol>
    h3 = <sol> <'###' space> words <eol>
    h4 = <sol> <'####' space> words <eol>
    h5 = <sol> <'#####' space> words <eol>
    h6 = <sol> <'######' space> words <eol>

    (* All three variants of thematic breaks *)
    hr = <( #'_{3,}' | #'[-]{3,}' | #'[*]{3,}' ) whitespace* eol> 

    (* Paragraphs can have up to 3 leading spaces *)
    p = (<sol> (space (space (space)?)?)? words <eol>)* | br

    <blocks> = ul / ol / blockquote / codefence / codeblock
    ul = (<sol> ul-li (<eol> ul-li <eol?>)*)+
    ol = (<sol> ol-li (<eol> ol-li <eol?>)*)+
    blockquote = (<sol> quote <eol>)+
    (* FIXME: Paragraphs takes presidence over codeblocks *)
    codeblock = (<sol> indentcode (<eol> indentcode)*)*
    <indentcode> = <space space space space+> words
    codefence = <sol> <fence space?> lang <eol> (<sol> words <eol>)+ <sol> <fence>
    ul-li = ul-mark (words | whitespace)*
    ol-li = <ol-mark> (words | whitespace)*
    <ul-mark> = <whitespace* ('-' | '*' | '+')> (<space> checkbox)? <whitespace+>
    <ol-mark> = <whitespace* digit+ ('.' | ')') whitespace+>
    checkbox = <'['> (space | 'X') <']'>
    <quote> = <'>'> (h1 | h2 | h3 | h4 | h5 | h6 | p | words)
    lang = word? words?
    <fence> = '```'

    img = image-mark words <']'> <'('> url <')'>
    <image-mark> = <'!['>
    link = <'['> words <']'> <'('> url <whitespace?> (<'\"'> #'[^\"]*' <'\"'>)? <')'>
    <url> = #'https?://[^ \n\t)]+'

    <digit> = #'[0-9]'

    <formatting> = code | strong | em | del
    code = <'`'> #'[^`]+' <'`'>
    strong = !ul-mark | <'**'> #'[^*]+' <'**'> | <'__'> #'[^_]+' <'__'>
    em = !ul-mark <'*'> #'[^*]+' <'*'> | <'_'> #'[^_]+' <'_'>
    del = <'~~'> #'[^~]+' <'~~'>

    <words> = word*
    br = <sol> whitespace* <eol>
    <word> = formatting | chars | whitespace

    (* Support for extended Latin, Cyrillic, Greek & Emoji's *)
    <chars> = !image-mark '!' | !ol-mark #'[\ud83c\udf00-\udfff|\ud83d\udc00-\ude4f\ude80-\udeff\udc00-\uddff\u0100-\u024F\u0400-\u04FF\u0500-\u052F\u2DE0-\u2DFF\uA640-\uA69F\u1D2B\u1D78\u00C5\u00E5\u00C6\u00E6\u00D8\u00F8a-zA-Z0-9,.:&=()#\"]+'
    <whitespace> = space | tab
    <space> = ' '
    <tab> = '\t'
    <sol> = #'(?m)\\A|^'
    <eol> = '\n' | '\r' | '\r\n'"
   :output-format :hiccup))

(defn- merge-consecutive-strings
  "Merges adjacent string elements in a sequence into a single string.
   This is used for concatenating parsed text nodes."
  [nodes]
  (reduce (fn [acc node]
            (if (and (string? (last acc)) (string? node))
              (conj (pop acc) (str (last acc) node))
              (conj acc node)))
          []
          nodes))

(def ^:private markdown-transformer
  (partial
   insta/transform
   {:img (fn [alt-text url] [:img {:src url :alt (apply str alt-text)}])
    :link (fn [text url title] [:a (merge {:href url :rel :noopener} (when title {:title title})) text])
    :ul-li (fn [& args] (into [:li] (merge-consecutive-strings args)))
    :ol-li (fn [& args] (into [:li] (merge-consecutive-strings args)))
    :p (fn [& args] [:p (merge-consecutive-strings (rest args))])
    :lang (fn [lang _] (when (seq lang) {:class [(str "language-" lang)]}))
    :codeblock (fn [body] [:pre [:code body]])
    :codefence (fn [lang & body] [:pre lang (into [:code] body)])
    :checkbox (fn [checked] [:input {:type :checkbox :checked (= checked "X")}])
    :markdown (fn [& args] (into [:<>] args))}))

(def ^:private hiccup-transformer
  (partial
   insta/transform
   {:h1 (fn [& text] (str "# " (apply str text)))
    :h2 (fn [& text] (str "## " (apply str text)))
    :h3 (fn [& text] (str "### " (apply str text)))
    :h4 (fn [& text] (str "#### " (apply str text)))
    :h5 (fn [& text] (str "##### " (apply str text)))
    :h6 (fn [& text] (str "###### " (apply str text)))
    :hr (fn [] (->> (repeat "-") (take 3) (str/join "")))
    :br (fn [] "\n")
    :em (fn [& text] (str "*" (apply str text) "*"))
    :strong (fn [& text] (str "**" (apply str text) "**"))
    :img (fn [{:keys [src alt]}] (str "[" alt "](" src ")"))
    :a (fn [{:keys [href title]} text] (str "[" text "](" href (when title (str " \"" title "\"")) ")"))
    :blockquote (fn [& args] (str "> " (str/join "\n> " args)))
    :ul (fn [& items] (str/join "\n" (map (fn [[_ & li]] (str "- " (apply str li))) items)))
    :ol (fn [& items] (str/join "\n" (map-indexed (fn [idx [_ & li]] (str (inc idx) ". " (apply str li))) items)))
    :<> (fn [& args] (str/join "\n" args))}))

(defn markdown->hiccup [& args]
  (let [[{:keys [on-failure]} markdown] (->params args)
        parsed (->> markdown
                    markdown-parser)]
    (if (insta/failure? parsed)
      (do (when (fn? on-failure)
            (on-failure (fail/pprint-failure parsed)))
          [:<>])
      (->> parsed markdown-transformer))))

(defn hiccup->markdown [hiccup]
  (->> hiccup
       hiccup-transformer))

(comment
  (def hiccup
    [:<>
     [:h1 "The " [:em "perfect"] " heading"]
     [:img {:alt "Some text" :src "http://example.com"}]
     [:a {:href "http://example.com" :title "Some title"} "And a text"]
     [:br]
     [:hr]
     [:ol
      [:li "Some " [:strong "item"]]
      [:li "Some " [:em "other item"]]]
     [:blockquote
      "This is the first line"
      "This is the second line"]])

  ;; hiccup -> markdown
  (hiccup->markdown hiccup)

  (def markdown
    (str/join "\n"
              ["## Hello *world*!"
               "[alt](http://example.org/)"
               "- [ ] List item 1"
               "- [X] List item 2"]))

  ;; markdown -> hiccup
  (markdown->hiccup markdown)

  ;; markdown -> ebnf
  (let [result (markdown-parser markdown)]
    (if (insta/failure? result)
      (throw (js/Error. (with-out-str (fail/pprint-failure result))))
      result)))