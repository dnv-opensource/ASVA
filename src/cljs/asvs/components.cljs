(ns asvs.components
  (:require [asvs.icons :as icons]
            [asvs.utils :refer [->params]]
            [clojure.string :as str]))

(defn progress-indicator [& args]
  (let [[params percent] (->params args)
        radius 45
        size (* (+ radius 5) 2)
        circumference (* 2 Math/PI radius)
        offset (* circumference (- 1 (/ percent 100)))]
    [:svg.Progress-indicator (merge {:viewBox (str "0 0 " size " " size)} params)
     [:circle.inactive {:cx (+ radius 5) :cy (+ radius 5) :r radius :fill "none"
                        :stroke-width "10"}]
     [:circle.active {:cx (+ radius 5) :cy (+ radius 5) :r radius :fill "none"
                      :stroke-width "10"
                      :stroke-dasharray (str circumference " " circumference)
                      :stroke-dashoffset (str offset)
                      :transform "rotate(-90 50 50)"}]
     [:title (str percent "%")]]))

(defn- -highlight
  "Returns a sequence of text segments based on the given `query`. Segments that
   match the query are wrapped in a `:mark` hiccup element for styling
   purposes. The function is case-insensitive in its matching."
  [text query]
  (if (> (count query) 2)
    (let [pattern (re-pattern (str "(?i)(" query ")"))
          parts (str/split text pattern)]
      (into [:<>]
            (map (fn [segment]
                   (if (re-matches pattern segment)
                     [:mark segment]
                     segment))
                 parts)))
    text))

(def highlight (memoize -highlight))

(defn switch [& args]
  (let [[params & body] (->params args)
        id (str/join "-" body)]
    [:div.Switch
     {:key (str "switch-" id)
      :class (into [(when (:disabled params) :disabled)] (:class params))}
     [:input (merge (dissoc params :class) {:type :checkbox})]]))

(defn checkbox [& args]
  (let [[params body] (->params args)
        id (str/join "-" body)]
    [:div.Checkbox
     {:key (str "check-" id)
      :class (into [(when (:disabled params) :disabled)] (:class params))}
     [:input (merge (dissoc params :class) {:id id :type :checkbox})]
     [icons/check]
     [:label {:for id} body]]))