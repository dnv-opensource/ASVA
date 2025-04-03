(ns hiccdown.core
  (:require
    [clojure.string :as str]
    [instaparse.core :as insta]
    [instaparse.failure :as fail]))

;; KNOWN ISSUES:
;; FIXME: Hiccup returned is empty when using a single quote in the markdown for `markdown->hiccup`
;; FIXME: Paragraphs takes presidence over indented code-blocks
;; FIXME: End of line problems with lists. Need two or more linebreaks to yield correct behavior
;; FIXME: Headings are parsed as regular text inside block-quotes
;; FIXME: Regression where setext headings no longer work

(def ^:private markdown-parser
  (insta/parser
    "markdown = element+
    <element> = headings | hr | img | link | blocks | p | br
    
    <headings> = h1 | h2 | h3 | h4 | h5 | h6
    h1 = <sol> <'#' space> words <eol> | <sol> words <eol sol #'={2,}' eol>
    h2 = <sol> <'##' space> words <eol> | <sol> words <eol sol #'-{2,}' eol>
    h3 = <sol> <'###' space> words <eol>
    h4 = <sol> <'####' space> words <eol>
    h5 = <sol> <'#####' space> words <eol>
    h6 = <sol> <'######' space> words <eol>

    hr = <( #'_{3,}' | #'[-]{3,}' | #'[*]{3,}' ) whitespace* eol> 

    p = (<sol> (space (space (space)?)?)? words <eol>)+
    br = <eol sol eol>

    <blocks> = ul | ol | blockquote | codefence | codeblock

    ul = (ul-li <eol> <sol>)*
    ol = (ol-li <eol> <sol>)*
    ul-li = <whitespace*> ul-mark words ( ul | ol )*
    ol-li = <whitespace*> <ol-mark> words ( ul | ol )*
    <ul-mark> = <('-' | '*' | '+')> (<space> checkbox)? <whitespace*>
    <ol-mark> = <digit+ ('.' | ')') whitespace*>
    checkbox = <'['> (space | 'X' | 'x') <']'>

    blockquote = (<sol> quote eol)+
    <quote> = <'>'> space (h1 | h2 | h3 | h4 | h5 | h6 | link | words)

    codeblock = (<sol> indentcode (<eol> indentcode)*)*
    <indentcode> = <space space space space+> words
    codefence = <sol> <fence space?> lang <eol> (<sol> words <eol>)+ <sol> <fence>
    lang = word? words?
    <fence> = '```'

    img = image-mark words <']'> <'('> url <')'>
    <image-mark> = <'!['>
    link = <'['> word* <']'> <'('> url <whitespace?> (<'\"'> #'[^\"]*' <'\"'>)? <')'>
    <url> = #'https?://[^ \n\t)]+'

    <digit> = #'[0-9]'

    <formatting> = code | strong | em | del
    code = <'`'> #'[^`]+' <'`'>
    strong = !ul-mark | <'**'> #'[^*]+' <'**'> | <'__'> #'[^_]+' <'__'>
    em = !ul-mark | <'*'> #'[^*]+' <'*'> | <'_'> #'[^_]+' <'_'>
    del = <'~~'> #'[^~]+' <'~~'>

    <words> = word+ 
    <word> = (formatting | link | chars)+ / whitespace

    (* Support for extended Latin, Cyrillic, Greek & Emoji's *)
    <chars> = !image-mark '!' | #'[a-zA-Z0-9-_,';\ud83c\udf00-\udfff|\ud83d\udc00-\ude4f\ude80-\udeff\udc00-\uddff\u0100-\u024F\u0400-\u04FF\u0500-\u052F\u2DE0-\u2DFF\uA640-\uA69F\u1D2B\u1D78\u00C5\u00E5\u00C6\u00E6\u00D8\u00F8.?:&=()#\"]+'
    <whitespace> = space | tab
    <space> = ' '
    <tab> = '\t'
    <sol> = #'(?m)^'
    <eol> = '\n' | '\r' | '\r\n'"
    :start :markdown
    :string-ci true
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
   {:img (fn [& args] [:img {:src (last args) :alt (apply str (butlast args))}])
    :link (fn [text url title] [:a (merge {:href url :rel :noopener} (when title {:title title})) text])
    :ul-li (fn [& args] (into [:li] (merge-consecutive-strings args)))
    :ol-li (fn [& args] (into [:li] (merge-consecutive-strings args)))
    :h1 (fn [& args] (into [:h1] (merge-consecutive-strings args)))
    :h2 (fn [& args] (into [:h2] (merge-consecutive-strings args)))
    :h3 (fn [& args] (into [:h3] (merge-consecutive-strings args)))
    :h4 (fn [& args] (into [:h4] (merge-consecutive-strings args)))
    :h5 (fn [& args] (into [:h5] (merge-consecutive-strings args)))
    :h6 (fn [& args] (into [:h6] (merge-consecutive-strings args)))
    :p (fn [& args] (into [:p] (merge-consecutive-strings args)))
    :blockquote (fn [& args] (into [:blockquote] (merge-consecutive-strings args)))
    :lang (fn [lang _] (when (seq lang) {:class [(str "language-" lang)]}))
    :codeblock (fn [body] [:pre (into [:code] (merge-consecutive-strings body))])
    :codefence (fn [lang & body] [:pre lang (into [:code] (merge-consecutive-strings body))])
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
    :img (fn [{:keys [src alt]}] (str "![" alt "](" src ")"))
    :a (fn [{:keys [href title]} text] (str "[" text "](" href (when title (str " \"" title "\"")) ")"))
    :blockquote (fn [& args] (str "> " (str/join "\n> " args)))
    :ul (fn [& items] (str/join "\n" (map (fn [[_ & li]] (str "- " (apply str li))) items)))
    :ol (fn [& items] (str/join "\n" (map-indexed (fn [idx [_ & li]] (str (inc idx) ". " (apply str li))) items)))
    :<> (fn [& args] (str/join "\n" args))}))

(defn- ->params
  "Normalize arguments to always have the form [props children] like
   hiccup elements."
  [args]
  (cond-> args
    (-> args first map? not) (conj nil)))

(defn markdown->hiccup [& args]
  (let [[{:keys [on-failure]} markdown] (->params args)]
    (when-let [parsed (and markdown (markdown-parser (str markdown "\n")))]
      (cond
        (insta/failure? parsed) (do (when (fn? on-failure)
                                      (on-failure (fail/pprint-failure parsed)))
                                    [:<>])
        :else (->> parsed markdown-transformer)))))

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
      ["# First heading with *emphasis*"
       "##### Fift level heading"
       "Regular text with **strong** and ~~strikethrough~~ emphasis."
       ""
       ""
       "Text that spans "
       "multiple"
       "lines."
       "****"
       ""
       "![some image](https://example.com)"
       ""
       "Example of an inline [link](https://example.com/). "
       "Note that links can have a title attribute [link with title](https://example.com/ \"Some title\")"
       ""
       "- Item 1"
       "- Item 2"
       "- Item 3"
       ""
       "- [ ] Task 1"
       "- [X] Task 2"
       ""
       "We can have inline `code`."
       "```clojure"
       "(str \"And of course, blocks of code\")"
       "```"
       ""
       "> We prefix lines with `>` to have block-quotes"
       "> Block quotes support all kinds of *formatting*, even"
       "> # Headings"]))

  ;; markdown -> hiccup
  (markdown->hiccup markdown)

  ;; markdown -> ebnf
  (let [result (markdown-parser markdown)]
    (if (insta/failure? result)
      (throw (js/Error. (with-out-str (fail/pprint-failure result))))
      result)))


(comment

  (markdown->hiccup "Works")

  ,)
