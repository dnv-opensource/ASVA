(ns asva.core
  (:require
   [asva.assessments :as assessments]
   [asva.components :refer [checkbox progress-indicator
                            upload-boundary]]
   [asva.i18n :refer [t]]
   [asva.icons :as icons]
   [asva.notifications :as notifications]
   [asva.store]
   [asva.utils :refer [->clj ->js <sub dispatch>e e> slug]]
   [goog.dom :as gdom]
   [re-frame.alpha :as re-frame]
   [reagent.core :as reagent]
   [reagent.dom.client :refer [create-root]]))

(defonce ^:private root
  (create-root (gdom/getElement "app")))

(re-frame/reg-event-fx
 :loading
  (fn [{:keys [db]} [_ v]]
    (let [f (if (= v :inc) inc dec)]
      {:db (update db :loading (fnil f 0))})))

(re-frame/reg-sub :loading?
  (fn [db _] (> (:loading db) 0)))

(defn header []
  (let [progress (<sub [::assessments/progress])
		     {:keys [title description]} (<sub [::assessments/introduction])]
	[:header
	 [:div
	  [progress-indicator {:width 256} progress]
	  (when (some? title)
            [:section.Introduction
             [:h1.DNV title]
             [:p.preface description]])]]))

(defn- search []
  (let [level-1 (<sub [::assessments/level-1])
        level-2 (<sub [::assessments/level-2])
        level-3 (<sub [::assessments/level-3])
        not-applicable? (<sub [::assessments/not-applicable?])
        completed? (<sub [::assessments/completed?])
        query (<sub [::assessments/query])]
    [:section.Search
     [:input {:type :search :value query :placeholder (t :search) :on-change (dispatch>e [::assessments/query value])}]
     [:footer
      [checkbox {:title (t :info-level-1) :checked level-1 :on-change (dispatch>e [::assessments/level-1 (not level-1)])} (t :level 1)]
      [checkbox {:title (t :info-level-2) :checked level-2 :on-change (dispatch>e [::assessments/level-2 (not level-2)])} (t :level 2)]
      [checkbox {:title (t :info-level-3) :checked level-3 :on-change (dispatch>e [::assessments/level-3 (not level-3)])} (t :level 3)]
      [checkbox {:checked not-applicable? :on-change (dispatch>e [::assessments/not-applicable? (not not-applicable?)])} (t :not-applicable)]
      [checkbox {:checked completed? :on-change (dispatch>e [::assessments/completed? (not completed?)])} (t :completed)]]]))

(defn- table-of-contents []
  (when-let [chapters (<sub [::assessments/chapters])]
    [:aside.Table-of-contents
     [:ol
      (for [{:keys [ordinal name short-name]} chapters]
        [:li {:key (slug :toc ordinal)}
         [:a {:title name
              :href (str (.-host js/location) "/#" ordinal)
              :on-click (e> (.preventDefault e)
                          (set! (.-hash js/location) ordinal))}
          short-name]])]]))

(defn- read-asvs-file [file]
  (re-frame/dispatch [:loading :inc])
  (let [reader (js/FileReader.)]
    (set! (.-onload reader) (dispatch>e (if-let [asvs (-> target .-result js/JSON.parse ->clj)]
                                          [::assessments/asvs asvs]
                                          [::notifications/add (t :failed-parsing)])))
    (set! (.-onerror reader) (dispatch>e [::notifications/add (t :failed-reading)]))
    (set! (.-onloadend reader) (dispatch>e [:loading :dec]))
    (.readAsText reader file)))

(defn- read-assessments-file [file]
  (re-frame/dispatch [:loading :inc])
  (let [reader (js/FileReader.)]
    (set! (.-onload reader) (dispatch>e (if-let [assessments-json (-> target .-result js/JSON.parse ->clj)]
                                          [::assessments/assessments assessments-json]
                                          [::notifications/add (t :failed-parsing)])))
    (set! (.-onerror reader) (dispatch>e [::notifications/add (t :failed-reading)]))
    (set! (.-onloadend reader) (dispatch>e [:loading :dec]))
    (.readAsText reader file)))

(defn- import-export []
  (let [asvas (<sub [::assessments/assessments])
        json (-> asvas ->js js/JSON.stringify)
        blob (js/Blob. #js [json] #js {"type" "application/json"})
        url (js/URL.createObjectURL blob)
        trigger-file-dialog (fn [e]
                              (.preventDefault e)
                              (.click (js/document.getElementById "hidden-file-input")))]
    [:div.Import-export
     [:input#hidden-file-input {:type :file
                                :name :file
                                :style {:display :none}
                                :accept [".json"]
                                :on-change (e> (read-assessments-file (-> target .-files first)))}]
     [:a {:href "/import-assessments" :on-click trigger-file-dialog :title (t :import-assessments)}
      [icons/upload]]
     (when (seq asvas)
       [:a {:href url :download (str "ASVA.json") :title (t :export-assessments)}
        [icons/download]])]))

(defn- app []
  [:main {:class (if (<sub [:loading?]) :loading :loaded)}
   [notifications/view]
   (if-let [all-assessments (<sub [::assessments/items])]
     [:<>
      [header]
      [search]
      [table-of-contents]
      [import-export]
      [assessments/view]]
     [upload-boundary {:accept [".json"]
		       :accept-types #{"application/json"}
		       :on-upload #(read-asvs-file (first %))}
      [:div
       [:h1.title.DNV (t :file-upload-title)]
       [:p.tagline.DNV (t :file-upload-intro)]]
      [icons/upload {:class [:large]}]
      [:strong (t :file-upload-info)]
      [:small (t :file-upload)]
      [:footer
       [:small (t :file-upload-help)
        [:a {:href "https://github.com/OWASP/ASVS/releases" :target "_blank"}
         (t :file-upload-download)]]]])])

(defn ^:dev/after-load main []
  (-> root (.render (reagent/as-element [app]))))

(defn ^:export init []
  (re-frame/dispatch-sync [::assessments/initialize])
  (re-frame/dispatch-sync [::notifications/initialize])
  (main))
