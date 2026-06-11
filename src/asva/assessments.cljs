(ns asva.assessments
  (:require
   [asva.components :refer [highlight progress-indicator recurrence-input]]
   [asva.i18n :refer [t]]
   [asva.icons :as icons]
   [asva.asvs :as asvs]
   [asva.notifications :as notifications]
   [asva.utils :refer [<> <sub dispatch>e e> parse-int slug slug-key now]]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.walk :refer [postwalk]]
   [hiccdown.core :refer [md->hiccup]]
   [re-frame.alpha :as re-frame]
   [reagent.core :as reagent]))

(<> [::asvs
     ::assessments
     ::level-1
     ::level-2
     ::level-3
     ::not-applicable?
     ::completed?
     ::query])

(declare enrich-nested-items)

(re-frame/reg-event-fx
 ::initialize
 [(re-frame/inject-cofx :local-storage)]
 (fn [{:keys [db local-storage]} _]
   {:db (merge db
               {::asvs (-> (get local-storage :asvs {})
                           asvs/normalize
                           enrich-nested-items)
                ::assessments (get local-storage :assessments {})
                ::level-1 true
                ::level-2 true
                ::level-3 true
                ::not-applicable? false
                ::completed? true})}))

(defn- chapter [shortcode]
  (try
    (-> shortcode
        name
        (subs 1)
        (str/split #"\.")
        first
        edn/read-string)
    (catch :default _
      (js/console.warn "Invalid shortcode format:" shortcode)
      nil)))

(def memoized-chapter (memoize chapter))

(defn enrich-item [item]
  (cond-> item
    (:shortcode item)
    (assoc :chapter (memoized-chapter (:shortcode item)))

    (and (not (:shortcode item)) (:req-id item))
    (assoc :chapter (memoized-chapter (:req-id item)))))

(defn enrich-nested-items
  "Apply `enrich-item` to every element in the innermost nested structure."
  [structure]
  (postwalk (fn [x] (if (map? x) (enrich-item x) x)) structure))

(re-frame/reg-event-fx
 ::asvs
 (fn [{:keys [db]} [k v]]
   (let [enriched-data (-> v asvs/normalize enrich-nested-items)
         item-count (asvs/item-count enriched-data)
         enriched-data (assoc enriched-data :import-status
                              {:asvs {:version (:version enriched-data)
                                      :items item-count
                                      :imported-at (now)}})]
     {:db (assoc db k enriched-data)
      :local-storage [k enriched-data]
      :dispatch [::notifications/add (t :asvs-imported item-count) :success 4000]})))

(re-frame/reg-event-fx
 ::associations
 (fn [{:keys [db]} [_ mapping]]
   (let [association-type (asvs/association-type mapping)
         association-count (count (asvs/associations mapping))
         enriched-data (-> db ::asvs (asvs/merge-associations mapping) enrich-nested-items
                           (assoc-in [:import-status :associations association-type]
                                     {:count association-count
                                      :imported-at (now)}))]
     (cond-> {:db (assoc db ::asvs enriched-data)
              :local-storage [::asvs enriched-data]}
       (pos? association-count)
       (assoc :dispatch [::notifications/add (t :associations-imported (name association-type) association-count) :success 4000])

       (zero? association-count)
       (assoc :dispatch [::notifications/add (t :no-associations-imported) :error])))))

(re-frame/reg-event-fx
 ::assessments
 (fn [{:keys [db]} [k v]]
   {:db (assoc db k v)
    :local-storage [k v]}))

(re-frame/reg-event-fx
 ::notes
 (fn [{:keys [db]} [_ shortcode notes]]
   (let [last-modified (now)
         updated-db (update-in db [::assessments (slug-key shortcode)]
                               (fn [assessment]
                                 (merge assessment
                                        {:asvs-version (-> db ::asvs :version)
                                         :notes notes
                                         :last-modified last-modified})))]
     {:db updated-db
      :local-storage [::assessments (get updated-db ::assessments)]})))

(re-frame/reg-event-fx
 ::progress
 (fn [{:keys [db]} [_ shortcode progress]]
   (let [progress (max 0 (min 100 progress))
         last-modified (now)
         updated-db (update-in db [::assessments (slug-key shortcode)]
                               (fn [assessment]
                                 (merge assessment
                                        {:asvs-version (-> db ::asvs :version)
                                         :progress progress
                                         :last-modified last-modified})))]
     {:db updated-db
      :local-storage [::assessments (get updated-db ::assessments)]})))

(re-frame/reg-event-fx
 ::not-applicable
 (fn [{:keys [db]} [_ shortcode not-applicable]]
   (let [last-modified (now)
         updated-db (update-in db [::assessments (slug-key shortcode)]
                               (fn [assessment]
                                 (merge assessment
                                        {:asvs-version (-> db ::asvs :version)
                                         :not-applicable not-applicable
                                         :last-modified last-modified})))]
     {:db updated-db
      :local-storage [::assessments (get updated-db ::assessments)]})))

(re-frame/reg-event-fx
 ::recurrence
 (fn [{:keys [db]} [_ shortcode recurrence]]
   (let [updated-db (update-in db [::assessments (slug-key shortcode)]
                               (fn [assessment]
                                 (merge assessment
                                        {:asvs-version (-> db ::asvs :version)
                                         :last-modified (now)
                                         :recurring recurrence})))]
     {:db updated-db
      :local-storage [::assessments (get updated-db ::assessments)]})))

;; Both ASVS files and the flat version have the `:requirements` keyword as entry-point.
(re-frame/reg-sub
 ::requirements
 :<- [::asvs]
 (fn [assessment]
   (get assessment :requirements)))

(re-frame/reg-sub
 ::has-asvs?
 :<- [::requirements]
 (fn [requirements]
   (boolean (seq requirements))))

(re-frame/reg-sub
 ::introduction
 :<- [::asvs]
 (fn [assessment]
   (when (some? (:name assessment))
     {:title (:name assessment)
      :description (:description assessment)})))

(re-frame/reg-sub
 ::import-status
 :<- [::asvs]
 (fn [assessment]
   (:import-status assessment)))

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
   (asvs/items {:requirements requirements})))

(defn match-string [query item]
  (str/includes? (str/lower-case (or item " ")) (str/lower-case query)))

(defn level-filter [level-1 level-2 level-3 item]
  (let [has-levels? (or (:l item)
                        (:l1 item) (:l2 item) (:l3 item)
                        (:level1 item) (:level2 item) (:level3 item))]
    (or (not has-levels?)
        (and (:l item)
             (or
              (and (true? level-1) (= (:l item) 1))
              (and (true? level-2) (= (:l item) 2))
              (and (true? level-3) (= (:l item) 3))))
        (and (true? level-1) (or (true? (-> item :l1 :required)) (= "✓" (:level1 item))))
        (and (true? level-2) (or (true? (-> item :l2 :required)) (= "✓" (:level2 item))))
        (and (true? level-3) (or (true? (-> item :l3 :required)) (= "✓" (:level3 item)))))))

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
                     (map #(if-let [assessment (get assessments (slug-key (:shortcode %)))] (merge % assessment) %))
                     (filter #(level-filter level-1 level-2 level-3 %))
                     (filter #(or not-applicable? (not (:not-applicable %))))
                     (filter #(not (and (not completed?) (= (:progress %) 100))))
                     (filter #(query-filter query %)))]
     (sequence transducer items))))

(re-frame/reg-sub
 ::progress
 :<- [::filtered-items]
 (fn [assessments]
   (letfn [(overall-progress [assessments]
             (let [extract-progress (map #(max 0 (get % :progress 0)))
                   total (transduce extract-progress + 0 assessments)
                   count (count assessments)]
               (if (> count 0)
                 (/ total count)
                 0)))]
     (overall-progress assessments))))

(re-frame/reg-sub
 ::grouped-items
 :<- [::filtered-items]
 (fn [items]
   ;; `:chapter` must be an int
   (let [items (->> items
                    (filter #(number? (:chapter %))) ; ignore broken items
                    (group-by :chapter))]
     (sort-by key items))))

(defn- as-list [x]
  (cond
    (nil? x) nil
    (coll? x) x
    :else [x]))

(defn assessment [{:keys [shortcode description progress not-applicable notes cwe nist last-modified asvs-version]}]
  (let [query (re-frame/subscribe [::query])
        recurrence-input? (reagent/atom false)
        editing? (reagent/atom false)
        note (reagent/atom notes)]
    (fn [{:keys [shortcode description progress not-applicable notes cwe nist last-modified asvs-version]}]
      (let [not-applicable (boolean not-applicable)
            url (str (-> js/location .-origin) "/#" shortcode)
            cwe-url (fn [n] (str "https://cwe.mitre.org/data/definitions/" n ".html"))
            nist-url (fn [_] "https://pages.nist.gov/800-63-3/sp800-63b.html")]
        [:div.Assessment {:id shortcode}
         [:span.item
          shortcode
          [:a {:title (t :copy-location)
               :href url :on-click (e> (.preventDefault e)
                                       (-> js/navigator .-clipboard (.writeText url)))}
           [icons/link {:class [:small]}]]
          [:div.not-applicable
           [:input {:id (str shortcode "-na")
                    :checked not-applicable
                    :type :checkbox
                    :title (t :not-applicable)
                    :on-change (dispatch>e [::not-applicable shortcode (not not-applicable)])}]
           [:label {:for (str shortcode "-na")} "n/a"]]]
         (when last-modified
           [:small.last-modified.badge {:title (t :registered-version asvs-version (js/Date. last-modified))}
            (t :last-modified (js/Date. last-modified))])
         [:p.description (highlight description @query)]
         [:div.horizontal
          (for [cwe (as-list cwe)]
            ^{:key (str shortcode "-cwe-" cwe)}
            [:a.badge.warning {:target :_blank :title :cwe :rel :noopener :href (cwe-url cwe)} cwe])
          (for [nist (as-list nist)]
            ^{:key (str shortcode "-nist-" nist)}
            [:a.badge {:target :_blank :title :nist :rel :noopener :href (nist-url nist)} nist])]
         [:menu
          (when-not @editing?
            [:button {:title (t :assessment-notes) :on-click (e> (reset! editing? true))} [icons/note]])
          [:div
           [:button {:title (t :recurring) :on-click #(swap! recurrence-input? not)} [icons/recurring]]]
          [recurrence-input {:class [(when-not @recurrence-input? :hide)] :on-change #(re-frame/dispatch [::recurrence shortcode %])}]]
         [:div.Markdown
          (if @editing?
            [:<>
             [:textarea {:auto-focus true :placeholder (t :assessment-notes) :value @note :on-change (e> (reset! note value))}]
             [:a {:href "https://www.markdownguide.org/basic-syntax/" :target :_blank :rel :noopener}
              [icons/markdown {:title (t :markdown-support)}]]
             [:button.primary {:on-click (dispatch>e (do (reset! editing? false) [::notes shortcode @note]))} "Save"]]
            (when (string? notes)
	      [md->hiccup notes]))]
         [progress-indicator {:width 40
                              :on-progress (fn [prog] (re-frame/dispatch [::progress shortcode prog]))} progress]]))))

(defn- scroll-to-hash []
  (let [hash (-> js/window.location.hash (subs 1))]
    (when-let [target (and (seq hash) (js/document.getElementById hash))]
      (js/scrollTo
       {:left 0
        :top (.-offsetTop target)
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
            (let [{:keys [name]} (some #(when (= (:ordinal %) n) %) chapters)]
              [:li {:key (slug :assessment n) :id n}
               [:h1.DNV name]
               (for [item items]
                 ^{:key (:shortcode item)}
                 [assessment item])]))]]))}))
