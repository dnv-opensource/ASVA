(ns asva.asvs
  (:require
   [clojure.walk :refer [postwalk]]
   [clojure.string :as str]))

(defn- numeric-parts [identifier]
  (some->> identifier
           (re-seq #"\d+")
           (mapv #?(:clj parse-long :cljs js/parseInt))))

(defn- natural-compare [a b]
  (compare [(or (numeric-parts (:shortcode a)) []) (:shortcode a)]
           [(or (numeric-parts (:shortcode b)) []) (:shortcode b)]))

(defn- cdx-standards [asvs]
  (or (-> asvs :declarations :standards)
      (-> asvs :definitions :standards)))

(defn- cdx-standard [asvs]
  (-> asvs cdx-standards first))

(defn cdx-format? [asvs]
  (boolean (and (map? asvs)
                (seq (cdx-standards asvs)))))

(defn- chapter-short-name [title]
  (-> (or title "")
      (str/split #",")
      first
      str/trim))

(defn- requirement-depth [{:keys [identifier]}]
  (count (numeric-parts identifier)))

(defn- level-requirement-set [levels level]
  (->> levels
       (filter #(= (str "Level " level) (:identifier %)))
       first
       :requirements
       set))

(defn- level-requirement-sets [levels]
  {:l1 (level-requirement-set levels 1)
   :l2 (level-requirement-set levels 2)
   :l3 (level-requirement-set levels 3)})

(defn- cdx-requirement->item [levels {:keys [identifier text]}]
  (let [{:keys [l1 l2 l3]} levels]
    (cond-> {:shortcode identifier
             :description text}
      (contains? l1 identifier) (assoc :l1 {:required true})
      (contains? l2 identifier) (assoc :l2 {:required true})
      (contains? l3 identifier) (assoc :l3 {:required true}))))

(defn- cdx-requirement->section [{:keys [identifier title]} items]
  {:shortcode identifier
   :ordinal (or (second (numeric-parts identifier)) 0)
   :name title
   :items (vec (sort natural-compare items))})

(defn- cdx-requirement->chapter [{:keys [identifier title]} sections]
  {:shortcode identifier
   :ordinal (or (first (numeric-parts identifier)) 0)
   :short-name (chapter-short-name title)
   :name title
   :items (vec (sort natural-compare sections))})

(defn- section-requirement? [requirement]
  (and (:title requirement) (= 2 (requirement-depth requirement))))

(defn- item-requirement? [requirement]
  (:text requirement))

(defn- normalize-cdx [asvs]
  (let [{:keys [name version description requirements levels]} (cdx-standard asvs)
        levels (level-requirement-sets levels)
        by-parent (group-by :parent requirements)
        parentless (get by-parent nil)
        chapters (filter #(= 1 (requirement-depth %)) parentless)]
    {:name name
     :version version
     :description description
     :requirements
     (->> chapters
          (map (fn [chapter]
                 (let [children (get by-parent (:identifier chapter))
                       sections (filter section-requirement? children)
                       direct-items (filter item-requirement? children)
                       section-items (cond-> (map (fn [section]
                                                    (cdx-requirement->section
                                                     section
                                                     (map (partial cdx-requirement->item levels)
                                                          (filter item-requirement?
                                                                  (get by-parent (:identifier section))))))
                                                  sections)
                                       (seq direct-items)
                                       (conj (cdx-requirement->section
                                              chapter
                                              (map (partial cdx-requirement->item levels) direct-items))))]
                   (cdx-requirement->chapter
                    chapter
                    section-items))))
          (sort natural-compare)
          vec)}))

(defn normalize
  "Converts supported ASVS JSON shapes into the nested structure ASVA uses."
  [asvs]
  (cond
    (cdx-format? asvs) (normalize-cdx asvs)
    :else asvs))

(defn items [asvs]
  (let [requirements (:requirements asvs)]
    (if (some? (-> requirements first :short-name))
      (->> requirements (mapcat :items) (mapcat :items))
      requirements)))

(defn item-count [asvs]
  (count (items asvs)))

(defn- shortcode [s]
  (when-let [identifier (some->> s str (re-seq #"\d+(?:\.\d+)+") last)]
    (str "V" identifier)))

(defn- present? [s]
  (not (str/blank? (str s))))

(defn- cwe-associations [mapping]
  (->> mapping
       (keep (fn [[k v]]
               (when-let [shortcode (shortcode k)]
                 [shortcode {:cwe v}])))
       (into {})))

(defn- split-nist-references [s]
  (->> (str/split (str s) #"\s*(?:,|/)\s*")
       (map str/trim)
       (filter present?)
       vec))

(defn- nist-associations [markdown]
  (->> (str/split-lines markdown)
       (keep (fn [line]
               (let [[_ requirement references] (re-matches #"\|\s*\*\*([^*]+)\*\*\s*\|\s*([^|]*?)\s*\|" line)]
                 (when (and requirement (present? references))
                   (when-let [shortcode (shortcode requirement)]
                     [shortcode {:nist (split-nist-references references)}])))))
       (into {})))

(defn associations
  "Converts known ASVS mapping files into shortcode-keyed item associations."
  [mapping]
  (cond
    (map? mapping) (cwe-associations mapping)
    (string? mapping) (nist-associations mapping)
    :else {}))

(defn association-type [mapping]
  (cond
    (map? mapping) :cwe
    (string? mapping) :nist
    :else :unknown))

(defn merge-associations [asvs mapping]
  (let [associations (associations mapping)]
    (postwalk
     (fn [x]
       (if-let [item-associations (and (map? x) (get associations (:shortcode x)))]
         (merge x item-associations)
         x))
     asvs)))
