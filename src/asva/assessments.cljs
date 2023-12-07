(ns asva.assessments
  (:require [asva.components :refer [highlight progress-indicator]]
            [asva.i18n :refer [t]]
            [asva.icons :as icons]
            [asva.utils :refer [<> <sub dispatch>e e> parse-int slug]]
            [clojure.string :as str]
            [hiccdown.core :refer [markdown->hiccup]]
            [re-frame.core :as re-frame]))

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

(re-frame/reg-event-fx
 ::asvs
 (fn [{:keys [db]} [k v]]
   {:db (assoc db k v)
    :local-storage [k v]}))

(re-frame/reg-event-fx
 ::assessments
 (fn [{:keys [db]} [k v]]
   {:db (assoc db k v)
    :local-storage [k v]}))

(re-frame/reg-event-fx
 ::notes
 (fn [{:keys [db]} [_ shortcode notes]]
   (let [updated (update-in db [::assessments] (fnil #(merge-with merge % {shortcode {:notes notes}}) {}))]
     {:db updated
      :local-storage [::assessments (get updated ::assessments)]})))

(re-frame/reg-event-fx
 ::progress
 (fn [{:keys [db]} [_ shortcode y-pos old-y-pos]]
   (let [updated (update-in db [::assessments] (fnil #(merge-with merge % {shortcode {:progress (min 100 (max 0 (- old-y-pos y-pos)))}}) {}))]
     {:db updated
      :local-storage [::assessments (get updated ::assessments)]})))

;; Both regular ASVS files and their flat counterpart has the `:requirements`
;; keyword as entry-point.
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
   (let [transducer (comp (filter #(level-filter level-1 level-2 level-3 %))
                          #_(filter #(not (and (false? not-applicable?) (neg? (:current-maturity %)))))
                          (filter #(not (and (not completed?) (= (:progress %) 1.0))))
                          (filter #(query-filter query %))
                          (map #(if-let [assessment (get assessments (:shortcode %))] (merge % assessment) %)))]
     (sequence transducer items))))

(re-frame/reg-sub
 ::progress
 :<- [::filtered-items]
 (fn [assessments]
   (letfn [(overall-progress [assessments]
             (let [extract-progress (map #(get % :progress 0))
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
   (let [items (group-by (if (some? (:chapter_id (first items))) :chapter_id :ordinal) items)]
     (->> items
          sort))))

;; TODO Update with link directly to each NIST item
(defn assessment [{:keys [shortcode description progress notes cwe nist last-updated]}]
  (let [progress (if (number? progress) progress 0)
        query (re-frame/subscribe [::query])
        url (str (-> js/location .-host) "/#" shortcode)
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
      (when (some? cwe) [:a.badge.warning {:target :_blank :rel :noopener :href (cwe-url cwe)} cwe])
      (when (some? nist) [:a.badge {:target :_blank :rel :noopener :href (nist-url nist)} nist])]
     [:div.Markdown
      (when notes [markdown->hiccup notes])
      [:textarea {:placeholder (t :assessment-notes) :value notes :on-change (dispatch>e [::notes shortcode value])}]
      [icons/markdown {:title (t :markdown-support)}]]
     [progress-indicator {:width 40 :on-drag (fn [y-pos old-y-pos] (re-frame/dispatch [::progress shortcode y-pos old-y-pos]))} progress]]))

(defn view []
  (let [chapters (<sub [::chapters])
        grouped-items (<sub [::grouped-items])]
    [:section.Assessments
     [:div.fade]
     [:ol
      (doall
       (for [[n items] grouped-items]
         (let [{:keys [name]} (nth chapters (dec n))]
           (into [:li {:key (slug :assessment n) :id n}
                  [:h1.DNV name]]
                 (map assessment items)))))]]))
