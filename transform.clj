(ns transform
  "Transforms our legacy ASVS JSON file to ASVA JSON file, compatible
  with the ASVA web-interface.
   
   Input format:
   A JSON array of objects where each object has multiple fields including
   'Item', 'CurrentMaturity', 'Comment', and 'Evidence'.
   
   Output format:
   A JSON object where each key is the target ASVS shortcode.
   The value for each key is an object that includes 'Progress' (renamed from
   'CurrentMaturity') and 'Notes' (a formatted string combining 'Comment' and
   'Evidence').

   When a v4.0.3 -> v5.0.0 mapping file is provided, progress is moved to the
   v5 controls. Deleted controls are skipped."
  (:require
   [cheshire.core :as json]
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   [java.nio.charset Charset StandardCharsets]
   [java.nio.file Files]))

(def default-mapping-file "resources/mapping_v4.0.3_to_v5.0.0.yml")
(def default-target-file
  "resources/OWASP_Application_Security_Verification_Standard_5.0.0_en.cdx.json")

(defn- read-text [file]
  (let [bytes (Files/readAllBytes (.toPath (io/file file)))
        unsigned #(bit-and (int %) 0xff)
        b0 (some-> bytes first unsigned)
        b1 (some-> bytes second unsigned)
        charset (cond
                  (and (= 0xff b0) (= 0xfe b1)) (Charset/forName "UTF-16LE")
                  (and (= 0xfe b0) (= 0xff b1)) (Charset/forName "UTF-16BE")
                  :else StandardCharsets/UTF_8)]
    (str/replace-first (String. bytes charset) #"^\uFEFF" "")))

(defn- shortcode [s]
  (when-let [identifier (some->> s str (re-seq #"\d+(?:\.\d+)+") last)]
    (str "V" identifier)))

(defn- target-shortcodes [status]
  (let [status (str status)]
    (when-not (str/includes? (str/upper-case status) "DELETED")
      (->> (re-seq #"(?i)v5\.0\.0-(\d+(?:\.\d+)+)" status)
           (map second)
           (map #(str "V" %))
           distinct
           vec))))

(defn- load-mapping [mapping-file]
  (if (and mapping-file (.exists (io/file mapping-file)))
    (->> (yaml/parse-string (slurp mapping-file) :keywords false)
         (keep (fn [[source target]]
                 (when-let [source-shortcode (shortcode source)]
                   [source-shortcode
                    (target-shortcodes (get target "tag-v5.0.0"))])))
         (into {}))
    {}))

(defn- remove-trailing-commas [s]
  (str/replace s #",(\s*[\]}])" "$1"))

(defn- force-json [x]
  (if (sequential? x) (doall x) x))

(defn- parse-json [s]
  (try
    (force-json (json/parse-string s true))
    (catch Exception _
      (force-json (json/parse-string (remove-trailing-commas s) true)))))

(defn- target-control-shortcodes [target-file]
  (when (and target-file (.exists (io/file target-file)))
    (->> (tree-seq coll? seq (parse-json (read-text target-file)))
         (keep (fn [x]
                 (when (map? x)
                   (or (when (:text x) (:identifier x))
                       (when (:description x) (:shortcode x))))))
         (keep shortcode)
         set)))

(defn transform-entry [entry]
  {:Progress (if (some? (:CurrentMaturity entry))
	       (* 100 (:CurrentMaturity entry))
	       0)
   :Notes (str (when (some? (:Comment entry))
		 (str "# Comments\n"
		   (str/join "\n" (:Comment entry))))
            (when (some? (:Evidence entry))
	      (str "\n\n# Evidence\n"
		(str/join "\n" (:Evidence entry)))))})

(defn- merge-assessment [old new]
  (if old
    {:Progress (max (or (:Progress old) 0) (or (:Progress new) 0))
     :Notes (str/join "\n\n---\n\n"
                      (remove str/blank? [(:Notes old) (:Notes new)]))}
    new))

(defn- entry-targets [mapping target-controls entry]
  (let [source (shortcode (:Item entry))]
    (cond->> (cond
               (contains? mapping source) (get mapping source)
               source [source]
               :else [])
      (seq target-controls) (filter target-controls))))

(defn transform-json
  ([input-file]
   (transform-json input-file default-mapping-file default-target-file))
  ([input-file mapping-file]
   (transform-json input-file mapping-file default-target-file))
  ([input-file mapping-file target-file]
   (let [json-data (parse-json (read-text input-file))
         mapping (load-mapping mapping-file)
         target-controls (target-control-shortcodes target-file)
         transformed (reduce (fn [acc entry]
                               (reduce (fn [acc target]
                                         (update acc target merge-assessment (transform-entry entry)))
                                       acc
                                       (entry-targets mapping target-controls entry)))
                             {}
                             json-data)]
     (json/generate-string transformed {:pretty true}))))

;; bb -m transform resources/ASVS-4.0.3.json ASVA.json resources/mapping_v4.0.3_to_v5.0.0.yml resources/OWASP_Application_Security_Verification_Standard_5.0.0_en.cdx.json
(defn -main [& args]
  (let [[input-file output-file mapping-file target-file] args
        mapping-file (or mapping-file default-mapping-file)
        target-file (or target-file default-target-file)
        transformed-json (transform-json input-file mapping-file target-file)]
    (if output-file
      (spit output-file transformed-json)
      (println transformed-json))))
