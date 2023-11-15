(ns asvs.store
  (:require
   [cljs-bean.core :refer [->clj ->js]]
   [re-frame.alpha :as re-frame]))

(re-frame/reg-fx
 :local-storage
 (fn [[key value]]
   (let [stored-value (into {} (some-> (.getItem js/localStorage "asvs")
                                       (js/JSON.parse)
                                       (->clj :keywordize-keys true)))]
     (.setItem js/localStorage "asvs" (.stringify js/JSON (->js (merge stored-value {key value})))))))

(re-frame/reg-cofx
 :local-storage
 (fn [coeffects _]
   (assoc coeffects :local-storage
          (into {} (some-> (.getItem js/localStorage "asvs")
                           (js/JSON.parse)
                           (->clj :keywordize-keys true))))))