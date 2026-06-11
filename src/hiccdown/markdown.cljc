(ns hiccdown.markdown
  (:require [clojure.string :as str]))

(def escapable
  #{\\ \` \* \_ \{ \} \[ \] \( \) \# \+ \- \. \! \| \~ \< \>})

(defn blank? [s]
  (or (nil? s) (str/blank? s)))

(defn split-lines [s]
  (str/split (str s) #"\r\n|\n|\r"))

(defn leading-spaces [s]
  (count (take-while #(= % \space) s)))

(defn trim-indent [n s]
  (subs s (min n (leading-spaces s))))

(defn escape-md [s]
  (-> (str s)
      (str/replace "\\" "\\\\")
      (str/replace #"([`*_{}\[\]()#+\-.!|~<>])" "\\\\$1")))

(defn escape-table-cell [s]
  (str/replace (escape-md s) "|" "\\|"))

(defn html-like? [s]
  (boolean (re-find #"(?is)^</?[A-Za-z][^>]*>$|^<!--.*-->$" (str/trim s))))
