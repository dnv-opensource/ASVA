(ns asva.utils
  (:require
    [clojure.string :as str]
    #?(:cljs [cljs-bean.core :as bean])
    #?(:cljs [re-frame.alpha :as re-frame]))
  #?(:cljs (:require-macros [asva.utils]))
  #?(:clj (:refer-clojure :exclude [slurp])))

(defn assoc-event-db
  "Associates a value into the given re-frame app-db at a specified path.

  Takes a re-frame `db` and `args`, where the last element of `args`
  is the value to associate, and the remaining elements define the
  nested keys at which to place that value.

  Usage:
  (re-frame/reg-event-db ::some-event assoc-event-db)

  Example:
  If `db` is {:a {:b {:c 0}}}, and `args` is [:a :b :c 1],
  the resulting db will be {:a {:b {:c 1}}}."
  [db args] (assoc-in db (pop args) (peek args)))

(defn ->params
  "Normalize arguments to always have the form [props children] like
   hiccup elements."
  [args]
  (cond-> args
    (-> args first map? not) (conj nil)))

(defn sanitize
  "Sanitizes the given string by:
  1. Removing all characters except alphanumeric characters, whitespace, and hyphens.
  2. Replacing one or more spaces with a single hyphen.
  3. Converting the string to lowercase."
  [s]
  (-> s
      (str/replace #"[^\w\s-]" "")
      (str/replace #"\s+" "-")
      (str/replace #"--+" "-")
      str/lower-case))

(defn slug
  "Generates a URL-friendly slug by joining the names of the provided arguments
   with hyphens. Any non-alphabetic characters in the resulting string are
   replaced with hyphens."
  [& args]
  (let [_slug (->> args
                (map (fn [s] (if (or (uuid? s) (number? s)) (str s) (name s))))
                (map sanitize)
                (str/join "-"))]
    (subs _slug 0 (min 250 (count _slug)))))

(defn slug-key
  "Generates a uniform lower-cased keyword from the provided strings."
  [& args]
  (-> (apply slug args)
    keyword))

#?(:cljs
   (defn <sub
     "Takes a re-frame subscription vector `sub` and returns its
       current value."
     [sub] (deref (re-frame/subscribe sub))))

#?(:clj
   (defmacro <>
     "Generates re-frame event and subscription handlers for each keyword in the given vector.

   The generated event handlers use `assoc-event-db` for updating the re-frame app-db.
   The generated subscription handlers use `get-in` to fetch the state based on the keyword.

   Usage:
   (<> [:key1 :key2 :key3])"
     [keywords]
     `(do
        ~@(mapcat (fn [k]
                    `[(re-frame/reg-event-db ~k assoc-event-db)
                      (re-frame/reg-sub ~k get-in)])
                  keywords))))

#?(:clj
   (defmacro slurp [file]
     (clojure.core/slurp file)))

#?(:clj
   (defmacro e>
     "Event handler macro. Makes (e)vent, target, value and innerHTML available."
     ([& body]
      `(fn e># [~'e]
         (let [~'target     (.-target ~'e)
               ~'value      (.-value ~'target)
               ~'inner-html (.-innerHTML ~'target)]
           ~@body)))))

;; REVIEW I’m reconsidering the utility of `dispatch>e`.
;;      | While `e>` is beneficial for general event-handler scenarios, `dispatch>e` is
;;      | limited to situations where a re-frame event is dispatched. This could be
;;      | efficiently handled with `(e> (dispatch [:key]))`, provided we adhere to the
;;      | convention of using `:refer` for `dispatch` rather than an `:as re-frame` alias
;;      | in the namespace declaration.
#?(:clj
   (defmacro dispatch>e
     "Event handler macro for Clojure that automatically dispatches a re-frame event.

      Uses the `e>` macro to destructure the event object, and then dispatches
      the specified re-frame event.

      Example:
      (dispatch>e [:event-id value])"
     [d]
     `(e> (re-frame/dispatch ~d))))

(defn parse-int [key]
  (#?(:clj Integer/parseInt :cljs js/parseInt) (re-find #"\d+" key)))

(defn pascal-to-kebab [s]
  (->> (re-seq #"[A-Z-_]+[a-z0-9-_\/\.]*" s)
       (map (fn [x]
              (cond
                (or (= x ".") (= x "/")) x
                :else (str/lower-case x))))
       (str/join "-")))

(defn kebab-to-pascal [s]
  (->> (str/split s #"-")
       (map str/capitalize)
       (apply str)))

(defn prop->key [prop]
  (keyword (pascal-to-kebab prop)))

(defn key->prop [key]
  (kebab-to-pascal (name key)))

#?(:cljs (defn now [] (.toISOString (js/Date.))))

#?(:cljs (defn ->clj [x] (bean/->clj x :prop->key prop->key :key->prop key->prop)))
#?(:cljs (defn ->js [x] (bean/->js x :prop->key prop->key :key->prop key->prop)))
