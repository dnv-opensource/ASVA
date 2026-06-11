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
              :href (str (.-origin js/location) "/#" ordinal)
              :on-click (e> (.preventDefault e)
                            (set! (.-hash js/location) ordinal))}
          short-name]])]]))

(defn- parse-json
  ([text] (parse-json text true))
  ([text warn?]
   (try
     (some-> text js/JSON.parse ->clj js->clj)
     (catch :default e
       (when warn?
         (js/console.warn "Failed to parse JSON file:" e))
       nil))))

(defn- read-text-file [file on-load]
  (when file
    (re-frame/dispatch [:loading :inc])
    (let [reader (js/FileReader.)]
      (set! (.-onload reader) (e> (on-load (.-result target))))
      (set! (.-onerror reader) (dispatch>e [::notifications/add (t :failed-reading)]))
      (set! (.-onloadend reader) (dispatch>e [:loading :dec]))
      (.readAsText reader file))))

(defn- read-asvs-file [file]
  (read-text-file
   file
   #(re-frame/dispatch-sync
     (if-let [asvs (parse-json %)]
       [::assessments/asvs asvs]
       [::notifications/add (t :failed-parsing)]))))

(defn- read-assessments-file [file]
  (read-text-file
   file
   #(re-frame/dispatch
     (if-let [assessments-json (parse-json %)]
       [::assessments/assessments assessments-json]
       [::notifications/add (t :failed-parsing)]))))

(defn- read-associations-file [file]
  (read-text-file
   file
   #(let [mapping (or (parse-json % false) %)]
      (re-frame/dispatch [::assessments/associations mapping]))))

(defn- download-json [filename json e]
  (.preventDefault e)
  (let [blob (js/Blob. #js [json] #js {"type" "application/json"})
        url (js/URL.createObjectURL blob)
        link (js/document.createElement "a")]
    (set! (.-href link) url)
    (set! (.-download link) filename)
    (.appendChild (.-body js/document) link)
    (.click link)
    (.remove link)
    (js/setTimeout #(js/URL.revokeObjectURL url) 0)))

(defn- import-status []
  (let [{:keys [asvs associations]} (<sub [::assessments/import-status])
        cwe (get-in associations [:cwe :count])
        nist (get-in associations [:nist :count])]
    (when (or asvs cwe nist)
      [:div.Import-status {:aria-live :polite}
       (when asvs
         [:span (t :asvs-status (:version asvs) (:items asvs))])
       (when cwe
         [:span (t :association-status "CWE" cwe)])
       (when nist
         [:span (t :association-status "NIST" nist)])])))

(defn- import-export []
  (let [asvas (<sub [::assessments/assessments])
        json (-> asvas ->js js/JSON.stringify)
        trigger-file-dialog (fn [id e]
                              (.preventDefault e)
                              (.click (js/document.getElementById id)))]
    [:div.Import-export
     [import-status]
     [:input#import-assessments-input {:type :file
                                       :name :file
                                       :style {:display :none}
                                       :accept [".json"]
                                       :on-change (e> (read-assessments-file (-> target .-files first)))}]
     [:input#import-associations-input {:type :file
                                        :name :file
                                        :style {:display :none}
                                        :accept [".json" ".md" ".markdown"]
                                        :on-change (e> (read-associations-file (-> target .-files first)))}]
     [:a {:href "/import-assessments"
          :aria-label (t :import-assessments)
          :on-click #(trigger-file-dialog "import-assessments-input" %)
          :title (t :import-assessments)}
      [icons/upload]]
     [:a {:href "/import-associations"
          :aria-label (t :import-associations)
          :on-click #(trigger-file-dialog "import-associations-input" %)
          :title (t :import-associations)}
      [icons/link]]
     (when (seq asvas)
       [:a {:href "/export-assessments"
            :aria-label (t :export-assessments)
            :on-click #(download-json "ASVA.json" json %)
            :title (t :export-assessments)}
        [icons/download]])]))

(defn- app []
  [:main {:class (if (<sub [:loading?]) :loading :loaded)}
   [notifications/view]
   (if (<sub [::assessments/has-asvs?])
     [:<>
      [header]
      [search]
      [table-of-contents]
      [import-export]
      [assessments/view]]
     [upload-boundary {:accept [".json" ".cdx.json"]
                       :accept-types #{"application/json"}
                       :on-upload #(read-asvs-file (first %))
                       :on-invalid #(re-frame/dispatch [::notifications/add (t :invalid-file-type) :error])}
      [:div
       [:h1.title.DNV (t :file-upload-title)]
       [:p.tagline.DNV (t :file-upload-intro)]]
      [icons/upload {:class [:large]}]
      [:strong (t :file-upload-info)]
      [:small (t :file-upload)]
      [:footer
       [:small (t :file-upload-help)
        [:a {:href "https://github.com/OWASP/ASVS/releases" :target "_blank" :rel :noopener}
         (t :file-upload-download)]]]])])

(defn ^:dev/after-load main []
  (-> root (.render (reagent/as-element [app]))))

(defn ^:export init []
  (re-frame/dispatch-sync [::assessments/initialize])
  (re-frame/dispatch-sync [::notifications/initialize])
  (main))
