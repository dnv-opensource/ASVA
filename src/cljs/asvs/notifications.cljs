(ns asvs.notifications
  (:require [asvs.icons :as icons]
            [asvs.utils :refer [<> <sub dispatch>e]]
            [re-frame.alpha :as re-frame]))

(re-frame/reg-event-db
 ::initialize
 (fn [db _]
   (merge db
          {::messages {}})))

(<> [::messages])

(re-frame/reg-event-fx
 ::add
 (fn [{:keys [db]} [_ message type delay]]
   (let [id (str (random-uuid))]
     (merge {:db (assoc-in db [::messages id] {:id id :message message :type type :delay delay})}
            (when delay {:dispatch-later [{:ms delay :dispatch [::remove id]}]})))))

(re-frame/reg-event-fx
 ::remove
 (fn [{:keys [db]} [_ id]]
   {:db (update db ::messages dissoc id)}))

(defn notification [[_ {:keys [id message type delay]}]]
  [:li.Notification {:key (str id) :id id :class [type]}
   (when (nil? delay)
     [:button {:on-click (dispatch>e [::remove id])} [icons/close]])
   message])

(defn view []
  (let [messages (<sub [::messages])]
    (into [:ul.Notifications] (map notification messages))))
