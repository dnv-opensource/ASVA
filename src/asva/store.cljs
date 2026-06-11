(ns asva.store
  (:require
   [asva.i18n :refer [t]]
   [asva.notifications :as notifications]
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
     (try
       (.setItem js/localStorage "asva" (.stringify js/JSON (->js (merge stored-value {key value}))))
       (catch :default e
         (js/console.warn "Failed to write ASVA local storage:" e)
         (re-frame/dispatch [::notifications/add (t :failed-storage) :error]))))))

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
