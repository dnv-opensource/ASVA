
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

(defn cloud-upload [params]
  (into [svg (merge-with into {:class [:cloud-upload]} params)]
        [[:path {:d "M7 18a4.6 4.4 0 0 1 0 -9a5 4.5 0 0 1 11 2h1a3.5 3.5 0 0 1 0 7h-1"}]
         [:path {:d "M9 15l3 -3l3 3"}]
         [:path {:d "M12 12l0 9"}]]))

(defn close [params]
  (into [svg (merge-with into {:class [:close]} params)]
        [[:path
          {:d "M12 2l.324 .001l.318 .004l.616 .017l.299 .013l.579 .034l.553 .046c4.785 .464 6.732 2.411 7.196 7.196l.046 .553l.034 .579c.005 .098 .01 .198 .013 .299l.017 .616l.005 .642l-.005 .642l-.017 .616l-.013 .299l-.034 .579l-.046 .553c-.464 4.785 -2.411 6.732 -7.196 7.196l-.553 .046l-.579 .034c-.098 .005 -.198 .01 -.299 .013l-.616 .017l-.642 .005l-.642 -.005l-.616 -.017l-.299 -.013l-.579 -.034l-.553 -.046c-4.785 -.464 -6.732 -2.411 -7.196 -7.196l-.046 -.553l-.034 -.579a28.058 28.058 0 0 1 -.013 -.299l-.017 -.616c-.003 -.21 -.005 -.424 -.005 -.642l.001 -.324l.004 -.318l.017 -.616l.013 -.299l.034 -.579l.046 -.553c.464 -4.785 2.411 -6.732 7.196 -7.196l.553 -.046l.579 -.034c.098 -.005 .198 -.01 .299 -.013l.616 -.017c.21 -.003 .424 -.005 .642 -.005zm-1.489 7.14a1 1 0 0 0 -1.218 1.567l1.292 1.293l-1.292 1.293l-.083 .094a1 1 0 0 0 1.497 1.32l1.293 -1.292l1.293 1.292l.094 .083a1 1 0 0 0 1.32 -1.497l-1.292 -1.293l1.292 -1.293l.083 -.094a1 1 0 0 0 -1.497 -1.32l-1.293 1.292l-1.293 -1.292l-.094 -.083z"
           :fill "currentColor"
           :stroke-width "0"}]]))

(defn comments [params]
  (into [svg (merge-with into {:class [:comments]} params)]
        [[:path {:d "M21 14l-3 -3h-7a1 1 0 0 1 -1 -1v-6a1 1 0 0 1 1 -1h9a1 1 0 0 1 1 1v10"}]
         [:path {:d "M14 15v2a1 1 0 0 1 -1 1h-7l-3 3v-10a1 1 0 0 1 1 -1h2"}]]))

(defn evidence [params]
  (into [svg (merge-with into {:class [:evidence]} params)]
        [[:path {:d "M12 9m-6 0a6 6 0 1 0 12 0a6 6 0 1 0 -12 0"}]
         [:path {:d "M12 15l3.4 5.89l1.598 -3.233l3.598 .232l-3.4 -5.889"}]
         [:path {:d "M6.802 12l-3.4 5.89l3.598 -.233l1.598 3.232l3.4 -5.889"}]]))