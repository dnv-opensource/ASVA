(ns asva.store
  (:require
    [asva.utils :refer [->clj ->js]]
    [re-frame.alpha :as re-frame]))

(re-frame/reg-fx
  :local-storage
  (fn [[key value]]
    (let [stored-value (try
                         (some-> (.getItem js/localStorage "asva")
                           js/JSON.parse
                           ->clj)
                         (catch :default e
                           (js/console.warn "Failed to read ASVA local storage:" e)
                           nil))]
      (.setItem js/localStorage "asva" (.stringify js/JSON (->js (merge stored-value {key value})))))))

(re-frame/reg-cofx
  :local-storage
  (fn [coeffects _]
    (let [res (assoc coeffects :local-storage
                (try
                  (some-> (.getItem js/localStorage "asva")
                    js/JSON.parse
                    ->clj
                    js->clj)
                  (catch :default e
                    (js/console.warn "Failed to read ASVA local storage:" e)
                    nil)))]
      res)))
