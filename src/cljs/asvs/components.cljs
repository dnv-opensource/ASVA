(ns asvs.components
  (:require
   [asvs.i18n :refer [t]]
   [asvs.icons :as icons]
   [asvs.utils :refer [->params e>]]
   [clojure.string :as str]
   [re-frame.loggers :as loggers]
   [reagent.core :as reagent]))

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
     [:title (t :percent percent)]]))

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

(defn checkbox [& args]
  (let [[params body] (->params args)
        id (str/join "-" body)]
    [:div.Checkbox
     {:key (str "check-" id)
      :class (into [(when (:disabled params) :disabled)] (:class params))}
     [:input (merge (dissoc params :class) {:id id :type :checkbox})]
     [icons/check]
     [:label {:for id} body]]))

(defn upload-boundary [& args]
  (let [[params & body] (->params args)
        {:keys [accept accept-types on-upload on-click]
         :or {on-click (fn [f] (f))}} params
        drag-over? (reagent/atom false)
        trigger-file-dialog (fn [] (.click (js/document.getElementById "hidden-file-input")))
        valid-file? (fn [file] (contains? accept (.-type file)))]
    (into [:div.Upload (merge {:on-drop (e> (let [files (-> e .-dataTransfer .-files array-seq)
                                                  valid-files (filter valid-file? files)]
                                              (doto e (.stopPropagation) (.preventDefault))
                                              (if (seq valid-files)
                                                (on-upload files)
                                                (.error js/console "Files you tried to upload are not valid"))
                                              (reset! drag-over? false)))
                               :on-drag-over (e> (do (doto e (.stopPropagation) (.preventDefault))
                                                     (reset! drag-over? true)))
                               :on-drag-leave (e> (do (doto e (.stopPropagation) (.preventDefault))
                                                      (reset! drag-over? false)))
                               :on-click #(on-click trigger-file-dialog)
                               :class [(when @drag-over? :drag-over)]}
                              (dissoc params :accept :accept-types :on-upload))
           [:input#hidden-file-input {:type :file
                                      :name :file
                                      :style {:display :none}
                                      :accept accept
                                      :on-change (e> (on-upload (-> target .-files)))}]
           (when @drag-over?
             [:div.Upload-message
              [:h1 (t :drop-to-upload)]])]
          body)))