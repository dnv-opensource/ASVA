(ns asvs.assessments
  (:require
   [asvs.components :refer [highlight progress-indicator]]
   [asvs.i18n :refer [t]]
   [asvs.icons :as icons]
   [asvs.utils :refer [<> <sub e> slug slurp]]
   [cljs-bean.core :refer [->clj]]
   [clojure.string :as str]
   [re-frame.alpha :as re-frame]))

(defn parse-int [key]
  (js/parseInt (re-find #"\d+" key)))

(defn camel-case-to-kebab-case [s]
  (if (->> s
           (re-seq #"[A-Z]")
           (apply str)
           (= s))
    (clojure.string/lower-case s)
    (->> s
         (re-seq #"[A-Z][^A-Z]*")
         (map clojure.string/lower-case)
         (clojure.string/join "-"))))


(defn translate-keys [m]
  (into {} (map (fn [[k v]] [(keyword (camel-case-to-kebab-case (name k))) v]) m)))

(defonce section-names
  {1 :architecture
   2 :authentication
   3 :session-management
   4 :access-control
   5 :validation-sanitization-encoding
   6 :cryptography
   7 :error-logging
   8 :data-protection
   9 :communications
   10 :malicious
   11 :business-logic
   12 :files-resources
   13 :api
   14 :config})

;; TODO Replace hard-coded filepath
;;    | We could use a file-input and the FileReader API. That way this could be
;;    | open-sourced and immediately usefull to others.
;;    | Could also hand the path to the app on boot or create a thin back-end.
(defonce assessment-data
  (->>  (-> (slurp "resources/ASVS-4.0.3.json")
            (str/replace #"[^A-Za-z0-9-# /\"'\.\\,_\{\}\[\]:]" "")
            (str/replace #"[ ]{1,}" " ")
            js/JSON.parse
            ->clj)
        (map translate-keys)
        (map (fn [itm]
               (some-> itm
                       (update :l1 (partial = "X"))
                       (update :l2 (partial = "X"))
                       (update :l3 (partial = "X")))))))

(re-frame/reg-event-db
 ::initialize
 (fn [_ _]
   {::assessments assessment-data
    ::level-1 true
    ::level-2 true
    ::level-3 true
    ::not-applicable? false
    ::completed? true}))

(<> [::assessments
     ::level-1
     ::level-2
     ::level-3
     ::not-applicable?
     ::completed?
     ::query])

(re-frame/reg-sub ::filtered-assessments get-in)

(defn match-string [query item]
  (str/includes? (str/lower-case (or item " ")) (str/lower-case query)))

(defn level-filter [level-1 level-2 level-3 assessment]
  (or (and (true? level-1) (true? (:l1 assessment)))
      (and (true? level-2) (true? (:l2 assessment)))
      (and (true? level-3) (true? (:l3 assessment)))))

(defn query-filter [query {:keys [description comment evidence]}]
  (if (not-empty query)
    (or (some #(match-string query %) comment)
        (some #(match-string query %) evidence)
        (match-string query description))
    true))

(re-frame/reg-flow
 {:id     ::filtered-assessments
  :inputs {:assessments [::assessments]
           :level-1     [::level-1]
           :level-2     [::level-2]
           :level-3     [::level-3]
           :not-applicable? [::not-applicable?]
           :completed? [::completed?]
           :query       [::query]}
  :output (fn filtered-assessments [_ {:keys [assessments level-1 level-2 level-3 not-applicable? completed? query]}]
            (let [transducer (comp (filter #(level-filter level-1 level-2 level-3 %))
                                   (filter #(not (and (false? not-applicable?) (neg? (:current-maturity %)))))
                                   (filter #(not (and (not completed?) (= (:current-maturity %) 1.0))))
                                   (filter #(query-filter query %)))]
              (sequence transducer assessments)))
  :path   [::filtered-assessments]})

(re-frame/reg-sub
 ::progress
 :<- [::filtered-assessments]
 (fn [assessments]
   (letfn [(overall-progress [assessments]
             (let [maturity-extractor (map :current-maturity)
                   total (transduce maturity-extractor + 0 assessments)
                   count (count assessments)]
               (if (> count 0)
                 (/ total count)
                 0)))]
     (overall-progress assessments))))

(re-frame/reg-sub
 ::grouped-assessments
 :<- [::filtered-assessments]
 (fn [assessments]
   (->> assessments
        (group-by :section)
        (map (fn [[k v]] [(parse-int k) v]))
        (sort))))

(defn assessment [{:keys [item description current-maturity comment evidence cwe]}]
  (let [query (re-frame/subscribe [::query])
        url (str (-> js/location .-host) "/#" item)
        comment-view (fn [text] [:p.comment (highlight text @query)])
        cwe-url (fn [n] (str "https://cwe.mitre.org/data/definitions/" n ".html"))]
    [:div.Assessment {:id item}
     [:span.item
      (str/replace item #"V" "")
      [:a {:title (t :copy-location)
           :href url :on-click (e> (.preventDefault e)
                                   (-> js/navigator .-clipboard (.writeText url)))}
       [icons/link {:class [:small]}]]]
     [:p.description (highlight description @query)]
     (when (not (str/blank? cwe)) [:a.badge {:target :_blank :rel :noopener :href (cwe-url cwe)} cwe])
     (into [:div.evidence] (map comment-view evidence))
     (into [:div.comments] (map comment-view comment))
     [progress-indicator {:width 40} (* current-maturity 100)]]))

(defn view []
  (let [grouped-assessments (<sub [::grouped-assessments])]
    [:section.Assessments
     [:div.fade]
     [:ol
      (for [[n assessments] grouped-assessments]
        (let [section (get section-names n)]
          (into [:li {:key (slug :assessment n) :id n}
                 [:h1.DNV (t section)]]
                (map assessment assessments))))]]))