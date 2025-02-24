(ns asva.store
  (:require
    [asva.utils :refer [->clj ->js]]
    [re-frame.alpha :as re-frame]))

(re-frame/reg-fx
  :local-storage
  (fn [[key value]]
    (let [stored-value (some-> (.getItem js/localStorage "asva")
                         js/JSON.parse
                         ->clj)]
      (.setItem js/localStorage "asva" (.stringify js/JSON (->js (merge stored-value {key value})))))))

(re-frame/reg-cofx
  :local-storage
  (fn [coeffects _]
    (let [res (assoc coeffects :local-storage
                (some-> (.getItem js/localStorage "asva")
                  js/JSON.parse
                  ->clj
                  js->clj))]
      res)))
