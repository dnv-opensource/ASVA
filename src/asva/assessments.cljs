(ns asva.assessments
  (:require
   [asva.components :refer [highlight progress-indicator]]
   [asva.i18n :refer [t]]
    [asva.icons :as icons]
    [asva.utils :refer [<> <sub dispatch>e e> parse-int slug]]
    [clojure.data :refer [diff]]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.walk :refer [postwalk]]
    [hiccdown.core :refer [markdown->hiccup]]
    [re-frame.core :as re-frame]
    [reagent.core :as reagent]))

(<> [::asvs
     ::assessments
     ::level-1
     ::level-2
     ::level-3
     ::not-applicable?
     ::completed?
     ::query])

(re-frame/reg-event-fx
  ::initialize
  [(re-frame/inject-cofx :local-storage)]
  (fn [{:keys [db local-storage]} _]
    {:db (merge db
	   {::asvs (get local-storage :asvs {})
	    ::assessments (get local-storage :assessments {})
	    ::level-1 true
	    ::level-2 true
	    ::level-3 true
	    ::not-applicable? false
	    ::completed? true})}))

(defn- chapter [shortcode]
  (-> shortcode (subs 1) (str/split ".") first edn/read-string))

(def memoized-chapter (memoize chapter))

(defn enrich-item [item]
  "Check item and enrich it if it contains :shortcode or :req-id."
  (cond
    (:shortcode item) (assoc item :chapter (memoized-chapter (:shortcode item)))
    (:req-id item) (assoc item :chapter (memoized-chapter (:req-id item)))
    :else item))

(defn enrich-nested-items [structure]
  "Apply `enrich-item` to every element in the innermost nested structure."
  (postwalk (fn [x] (if (map? x) (enrich-item x) x)) structure))

(re-frame/reg-event-fx
  ::asvs
  (fn [{:keys [db]} [k v]]
    (let [enriched-data (enrich-nested-items v)]
      {:db (assoc db k enriched-data)
       :local-storage [k enriched-data]})))


(re-frame/reg-event-fx
  ::assessments
  (fn [{:keys [db]} [k v]]
    {:db (assoc db k v)
    :local-storage [k v]}))

(re-frame/reg-event-fx
  ::notes
  (fn [{:keys [db]} [_ shortcode notes]]
    (let [updated-db (assoc-in db [::assessments shortcode :notes] notes)]
      {:db updated-db
       :local-storage [::assessments (get updated-db ::assessments)]})))

(re-frame/reg-event-fx
  ::progress
  (fn [{:keys [db]} [_ shortcode progress]]
     (let [updated-db (assoc-in db [::assessments shortcode :progress] progress)]
       {:db updated-db
	:local-storage [::assessments (get updated-db ::assessments)]})))

;; Both ASVS files and the flat version have the `:requirements` keyword as entry-point.
(re-frame/reg-sub
 ::requirements
 :<- [::asvs]
 (fn [assessment]
   (get assessment :requirements)))

(re-frame/reg-sub
 ::introduction
 :<- [::asvs]
 (fn [assessment]
   (when (some? (:name assessment))
     {:title (:name assessment)
      :description (:description assessment)})))

(re-frame/reg-sub
 ::chapters
 :<- [::requirements]
 (fn [requirements]
   (map (fn [requirement]
          (cond
            (:chapter-id requirement) {:ordinal (parse-int (:chapter-id requirement))
                                       :name (:chapter-name requirement)
                                       :short-name (-> (:chapter-name requirement) (str/split #",") first)}
            :else (select-keys requirement [:ordinal :name :short-name]))) requirements)))

(re-frame/reg-sub
 ::items
 :<- [::requirements]
 (fn [requirements]
   (if (some? (-> requirements first :short-name))
     (->> requirements (mapv :items) flatten (mapv :items) flatten)
     requirements)))

(defn match-string [query item]
  (str/includes? (str/lower-case (or item " ")) (str/lower-case query)))

(defn level-filter [level-1 level-2 level-3 item]
  (or (and (true? level-1) (or (true? (-> item :l1 :required)) (= "✓" (:level1 item))))
      (and (true? level-2) (or (true? (-> item :l2 :required)) (= "✓" (:level2 item))))
      (and (true? level-3) (or (true? (-> item :l3 :required)) (= "✓" (:level3 item))))))

(defn query-filter [query {:keys [description req-description]}]
  (if (not-empty query)
    (or (match-string query req-description)
        (match-string query description))
    true))

(re-frame/reg-sub
  ::filtered-items
  :<- [::items]
  :<- [::assessments]
  :<- [::level-1]
  :<- [::level-2]
  :<- [::level-3]
  :<- [::not-applicable?]
  :<- [::completed?]
  :<- [::query]
  (fn [[items assessments level-1 level-2 level-3 not-applicable? completed? query]]
    (let [transducer (comp
		       (map #(if-let [assessment (get assessments (:shortcode %))] (merge % assessment) %))
		       (filter #(level-filter level-1 level-2 level-3 %))
		       (filter #(not (and (false? not-applicable?) (neg? (:progress %)))))
		       (filter #(not (and (not completed?) (= (:progress %) 100))))
		       (filter #(query-filter query %)))]
      (sequence transducer items))))

(re-frame/reg-sub
 ::progress
 :<- [::filtered-items]
 (fn [assessments]
   (letfn [(overall-progress [assessments]
             (let [extract-progress (map #(get % :progress 0))
                   total (transduce (max 0 extract-progress) + 0 assessments)
                   count (count assessments)]
               (if (> count 0)
                 (/ total count)
                 0)))]
     (overall-progress assessments))))

(re-frame/reg-sub
 ::grouped-items
  :<- [::filtered-items]
  (fn [items]
    (let [items (group-by :chapter items)]
      (->> items sort))))

(defn assessment [{:keys [shortcode description progress notes cwe nist last-updated]}]
  (let [query (re-frame/subscribe [::query])
        editing? (reagent/atom false)
        note (reagent/atom notes)]
    (fn [{:keys [shortcode description progress notes cwe nist last-updated]}]
      (let [url (str (-> js/location .-host) "/#" shortcode)
            cwe-url (fn [n] (str "https://cwe.mitre.org/data/definitions/" n ".html"))
            nist-url (fn [_] (str "https://csrc.nist.gov/pubs/sp/800/53/b/upd1/final"))
            cwe (if (coll? cwe) (first cwe) cwe)
            nist (if (coll? nist) (first nist) nist)]
        [:div.Assessment {:id shortcode}
	 [:span.item
	  shortcode
	  [:a {:title (t :copy-location)
	       :href url :on-click (e> (.preventDefault e)
				     (-> js/navigator .-clipboard (.writeText url)))}
	   [icons/link {:class [:small]}]]]

	 (when last-updated [:small.last-updated.badge (t :last-updated last-updated)])
	 [:p.description (highlight description @query)]
	 [:div.horizontal
	  (when (some? cwe) [:a.badge.warning {:target :_blank :title :cwe :rel :noopener :href (cwe-url cwe)} cwe])
	  (when (some? nist) [:a.badge {:target :_blank :title :nist :rel :noopener :href (nist-url nist)} nist])]
	 [:div.Markdown
	  (if @editing?
	    [:<>
	     [:textarea {:auto-focus true :placeholder (t :assessment-notes) :value @note :on-change (e> (reset! note value))}]
	     [:a {:href "https://www.markdownguide.org/basic-syntax/" :target :_blank :rel :noopener}
	      [icons/markdown {:title (t :markdown-support)}]]]
	    [markdown->hiccup notes])]
	 (if @editing?
	   [:button.primary {:on-click (dispatch>e (do (reset! editing? false) [::notes shortcode @note]))} "Save"]
	   [:button {:title (t :assessment-notes) :on-click (e> (reset! editing? true))} [icons/note]])
	 [progress-indicator {:width 40
			      :on-progress (fn [prog] (re-frame/dispatch [::progress shortcode prog]))} progress]]))))

(defn- scroll-to-hash []
  (let [hash (-> js/window.location.hash (subs 1))]
    (when (not (empty? hash))
      (js/scrollTo
	{:left 0
	 :top (.-offsetTop (js/document.getElementById hash))
	 :behavior :smooth}))))

(defn view []
  (reagent/create-class
    {:component-did-mount scroll-to-hash
     :reagent-render
     (fn []
       (let [chapters (<sub [::chapters])
	     grouped-items (<sub [::grouped-items])]
	 [:section.Assessments
	  [:div.fade]
	  [:ol
	   (for [[n items] grouped-items]
	     (let [{:keys [name]} (nth chapters (dec n))]
	       [:li {:key (slug :assessment n) :id n}
		[:h1.DNV name]
		(for [item items]
		  ^{:key (:shortcode item)} [assessment item])]))]]))}))
