(ns transform
  "Transforms our legacy ASVS JSON file to ASVA JSON file, compatible
  with the ASVA web-interface.
   
   Input format:
   A JSON array of objects where each object has multiple fields including
   'Item', 'CurrentMaturity', 'Comment', and 'Evidence'.
   
   Output format:
   A JSON object where each key is the 'Item' value of the input objects.
   The value for each key is an object that includes 'Progress' (renamed from
   'CurrentMaturity') and 'Notes' (a formatted string combining 'Comment' and
   'Evidence')."
  (:require
   [babashka.fs :as fs]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.java.io :as io]
    [cheshire.core :as json]))

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

(defn transform-json [input-file]
  (let [json-data (json/parse-string (slurp input-file) true)
	transformed (reduce (fn [acc entry]
			      (assoc acc (:Item entry) (transform-entry entry)))
		      {}
		      json-data)]
    (json/generate-string transformed)))

;; bb -m transform ASVS-4.0.3-STC.json ASVA.json
(defn -main [& args]
  (let [input-file (first args)
	output-file (second args)
	transformed-json (transform-json input-file)]
	(if output-file
	  (spit output-file transformed-json)
	  (println transformed-json))))
