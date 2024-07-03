(ns asva.icons
  (:require
   [asva.utils :refer [->params]]
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

(defn close [params]
  (into [svg (merge-with into {:class [:close]} params)]
        [[:path
          {:d "M12 2l.324 .001l.318 .004l.616 .017l.299 .013l.579 .034l.553 .046c4.785 .464 6.732 2.411 7.196 7.196l.046 .553l.034 .579c.005 .098 .01 .198 .013 .299l.017 .616l.005 .642l-.005 .642l-.017 .616l-.013 .299l-.034 .579l-.046 .553c-.464 4.785 -2.411 6.732 -7.196 7.196l-.553 .046l-.579 .034c-.098 .005 -.198 .01 -.299 .013l-.616 .017l-.642 .005l-.642 -.005l-.616 -.017l-.299 -.013l-.579 -.034l-.553 -.046c-4.785 -.464 -6.732 -2.411 -7.196 -7.196l-.046 -.553l-.034 -.579a28.058 28.058 0 0 1 -.013 -.299l-.017 -.616c-.003 -.21 -.005 -.424 -.005 -.642l.001 -.324l.004 -.318l.017 -.616l.013 -.299l.034 -.579l.046 -.553c.464 -4.785 2.411 -6.732 7.196 -7.196l.553 -.046l.579 -.034c.098 -.005 .198 -.01 .299 -.013l.616 -.017c.21 -.003 .424 -.005 .642 -.005zm-1.489 7.14a1 1 0 0 0 -1.218 1.567l1.292 1.293l-1.292 1.293l-.083 .094a1 1 0 0 0 1.497 1.32l1.293 -1.292l1.293 1.292l.094 .083a1 1 0 0 0 1.32 -1.497l-1.292 -1.293l1.292 -1.293l.083 -.094a1 1 0 0 0 -1.497 -1.32l-1.293 1.292l-1.293 -1.292l-.094 -.083z"
           :fill "currentColor"
           :stroke-width "0"}]]))

(defn note [params]
  (into [svg (merge-with into {:class [:note]} params)]
        [[:path {:d "M7 7h-1a2 2 0 0 0 -2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2 -2v-1"}]
         [:path
          {:d "M20.385 6.585a2.1 2.1 0 0 0 -2.97 -2.97l-8.415 8.385v3h3l8.385 -8.415z"}]
         [:path {:d "M16 5l3 3"}]]))

(defn markdown [params]
  (into [svg (merge-with into {:class [:markdown]} params)]
        [[:path {:d "M3 5m0 2a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-14a2 2 0 0 1 -2 -2z"}]
         [:path {:d "M7 15v-6l2 2l2 -2v6"}]
         [:path {:d "M14 13l2 2l2 -2m-2 2v-6"}]]))

(defn download [params]
  (into [svg (merge-with into {:class [:export]} params)]
        (if (not (:alt params))
          [[:path {:d "M14 3v4a1 1 0 0 0 1 1h4"}]
           [:path
            {:d "M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z"}]
           [:path {:d "M12 17v-6"}]
           [:path {:d "M9.5 14.5l2.5 2.5l2.5 -2.5"}]]
          [[:path {:d "M14 3v4a1 1 0 0 0 1 1h4"}]
           [:path {:d "M11.5 21h-4.5a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v5m-5 6h7m-3 -3l3 3l-3 3"}]])))

(defn upload [params]
  (into [svg (merge-with into {:class [:import]} params)]
        [[:path {:d "M14 3v4a1 1 0 0 0 1 1h4"}]
         [:path {:d "M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z"}]
         [:path {:d "M12 11v6"}]
         [:path {:d "M9.5 13.5l2.5 -2.5l2.5 2.5"}]]))
