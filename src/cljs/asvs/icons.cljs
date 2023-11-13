
(ns asvs.icons
  (:require [asvs.utils :refer [->params]]
            [clojure.string :as str]))

(def ^:private size 24)

(defn- svg [& args]
  (let [[{:keys [title class height width] :as params
          :or {height size width size}} & body] (->params args)]
    [:span.Icon {:title title :class class}
     (into [:svg
            (merge-with into
                        {:key (str "icon-" (str/join "-" class))
                         :xmlns "http://www.w3.org/2000/svg"
                         :stroke :currentColor
                         :fill :none
                         :stroke-linejoin :round
                         :stroke-linecap :round
                         :stroke-width 2
                         :height height
                         :width width
                         :viewBox (str/join " " [0 0 24 24])}
                        (dissoc params :title :class))] body)]))

(defn link [params]
  [svg params
   [:path {:d "M9 15l6 -6"}]
   [:path {:d "M11 6l.463 -.536a5 5 0 0 1 7.071 7.072l-.534 .464"}]
   [:path {:d "M13 18l-.397 .534a5.068 5.068 0 0 1 -7.127 0a4.972 4.972 0 0 1 0 -7.071l.524 -.463"}]])

(defn check [params]
  (into [svg (merge-with into {:class [:check]} params)]
        [[:path {:d "M5 12l5 5l10 -10"}]]))