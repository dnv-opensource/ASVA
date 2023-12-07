(ns asva.store
  (:require
   [asva.utils :refer [->clj ->js]]
   [re-frame.core :as re-frame]))

(re-frame/reg-fx
 :local-storage
 (fn [[key value]]
   (let [stored-value (into {} (some-> (.getItem js/localStorage "asva")
                                       (js/JSON.parse)
                                       ->clj))]
     (.setItem js/localStorage "asva" (.stringify js/JSON (->js (merge stored-value {key value})))))))

(re-frame/reg-cofx
 :local-storage
 (fn [coeffects _]
   (assoc coeffects :local-storage
          (into {} (some-> (.getItem js/localStorage "asva")
                           (js/JSON.parse)
                           ->clj)))))