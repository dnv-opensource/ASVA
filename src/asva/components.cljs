(ns asva.components
  (:require
   [asva.i18n :refer [t]]
   [asva.icons :as icons]
   [asva.utils :refer [->params e>]]
   [clojure.string :as str]
   [reagent.core :as reagent]
   [reagent.core :as r]))

(defn progress-indicator [& args]
  (let [[{:keys [on-progress width]
	  :or {width 96} :as params} percent] (->params args)
	not-applicable? (= percent -1)
	stroke-width (max 6 (* width 0.1))
	radius (- width stroke-width)
	svg-center (+ radius stroke-width)
	size (+ (* svg-center 2) (* stroke-width 2))
	circumference (* 2 Math/PI radius)
	initial-offset circumference
	progress-offset (* circumference (/ (min 100 (max 0 (if (js/isNaN percent) 0 percent))) 100))
	final-offset (- initial-offset progress-offset)
	dragging (atom false)
	start-coord (atom nil)
	mouse-up-ref (atom nil)
	handle-on-click (fn []
			  (cond
			    (< percent 0) (on-progress 0)
			    (< percent 25)  (on-progress 25)
			    (< percent 50)  (on-progress 50)
			    (< percent 75)  (on-progress 75)
			    (< percent 100) (on-progress 100)
			    (= percent 100) (on-progress -1)
			    :else           (on-progress 0)))

	handle-mouse-move (e> (when @dragging
				(let [y-pos (-> e .-clientY)
				      old-y-pos @start-coord]
				  (on-progress (if (nil? old-y-pos) y-pos (- old-y-pos y-pos))))))
	handle-mouse-up (e>
			  (when (= @start-coord (-> e .-clientY))
			    (handle-on-click))
			  (reset! dragging false)
			  (reset! start-coord nil)
			  (.removeEventListener js/document "mousemove" handle-mouse-move)
			  (.removeEventListener js/document "mouseup" @mouse-up-ref)
			  (reset! mouse-up-ref nil))
	handle-mouse-down (e> (reset! dragging true)
			    (reset! start-coord (-> e .-clientY))
			    (.addEventListener js/document "mousemove" handle-mouse-move)
			    (reset! mouse-up-ref (.addEventListener js/document "mouseup" handle-mouse-up)))]
    [:svg.Progress-indicator (merge {:viewBox (str "0 0 " size " " size)
				     :on-mouse-down handle-mouse-down
				     :class (when not-applicable? :not-applicable)}
			       params)
     [:defs
      [:filter#glow [:feGaussianBlur {:stdDeviation 3 :result "coloredBlur"}]
       [:feMerge
	[:feMergeNode {:in "coloredBlur"}]
	[:feMergeNode {:in "SourceGraphic"}]]]]
     [:circle.inactive {:cx svg-center :cy svg-center :r radius :fill "none"
			:stroke-width stroke-width}]
     [:rect {:x stroke-width :y (dec svg-center) :width (- size (* stroke-width 4)) :height stroke-width}]
     [:circle.active {:cx svg-center :cy svg-center :r radius :fill "none"
		      :stroke-width stroke-width
		      :stroke-dasharray (str circumference " " circumference)
		      :stroke-dashoffset (str final-offset)}]
     [:title (if not-applicable?
	       "N/A"
	       (str percent "%"))]]))

(defn- -highlight
       "Returns a sequence of text segments based on the given `query`. Segments that
   match the query are wrapped in a `:mark` hiccup element for styling
   purposes."
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
        id (str/join "-" body)
        title (or (:title params) "")]
    [:div.Checkbox
     {:key (str "check-" id)
      :class (into [(when (:disabled params) :disabled)] (:class params))}
     [:input (merge (dissoc params :class) {:id id :type :checkbox})]
     [icons/check]
     [:label {:for id :title title} body]]))

(defn upload-boundary [& args]
  (let [[params & body] (->params args)
        {:keys [accept accept-types on-upload on-click]
         :or {on-click (fn [f e] (f e))}} params
        drag-over? (reagent/atom false)
        trigger-file-dialog (fn [e] (if (= "A" e.target.tagName)
                                      (e.stopPropagation())
                                     (.click (js/document.getElementById "hidden-file-input"))))
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
                               :on-click #(on-click trigger-file-dialog %)
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

(defn recurrence-input [{:keys [value on-change] :as params}]
  (let [{:keys [every unit]
         :or {every 0 unit :month}} value
        local-state (r/atom {:every every :unit unit})]
    (fn [{:keys [on-change] :as params}]
      (let [params (dissoc params :on-change :value)]
       [:div.Recurrence-input params
        [:div
         [:label (t :every)]
         [:input {:type      :number
                  :min       0
                  :size      3
                  :value     (:every @local-state)
                  :on-change (fn [e]
                               (let [v (js/parseInt (.. e -target -value))]
                                 (swap! local-state assoc :every v)
                                 (on-change (assoc @local-state :every v))))}]]
        [:div
         [:label (t :time-span)]
         [:select {:value     (name (:unit @local-state))
                   :on-change (fn [e]
                                (let [v (keyword (.. e -target -value))]
                                  (swap! local-state assoc :unit v)
                                  (on-change (assoc @local-state :unit v))))}
          [:option {:value :day} "Day"]
          [:option {:value :week} "Week"]
          [:option {:value :month} "Month"]
          [:option {:value :year} "Year"]]]]))))
