(ns asvs.core
  (:require [asvs.assessments :as assessments]
            [asvs.components :refer [checkbox progress-indicator]]
            [asvs.i18n :refer [t]]
            [asvs.utils :refer [<sub dispatch>e e> slug]]
            [goog.dom :as gdom]
            [re-frame.alpha :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom.client :refer [create-root]]))

(defonce root
  (create-root (gdom/getElement "app")))

(defn introduction []
  [:section.Introduction
   [:h1.DNV (t :main-title)]
   [:p.preface
    (t :preface-1)
    [:a {:target :_blank :rel :noopener :href "https://owasp.org/www-project-application-security-verification-standard/"} (t :preface-link-1)]
    (t :preface-2)
    [:a {:target :_blank :rel :noopener :href "https://www.owasp.org/index.php/OWASP_Proactive_Controls"} (t :preface-link-2)]
    (t :preface-3)]])

(defn search []
  (let [level-1 (<sub [::assessments/level-1])
        level-2 (<sub [::assessments/level-2])
        level-3 (<sub [::assessments/level-3])
        not-applicable? (<sub [::assessments/not-applicable?])
        completed? (<sub [::assessments/completed?])
        query (<sub [::assessments/query])]
    [:section.Search
     [:input {:type :search :value query :placeholder (t :search) :on-change (dispatch>e [::assessments/query value])}]
     [:footer
      [checkbox {:checked level-1 :on-change (dispatch>e [::assessments/level-1 (not level-1)])} (t :level 1)]
      [checkbox {:checked level-2 :on-change (dispatch>e [::assessments/level-2 (not level-2)])} (t :level 2)]
      [checkbox {:checked level-3 :on-change (dispatch>e [::assessments/level-3 (not level-3)])} (t :level 3)]
      [checkbox {:checked not-applicable? :on-change (dispatch>e [::assessments/not-applicable? (not not-applicable?)])} (t :not-applicable)]
      [checkbox {:checked completed? :on-change (dispatch>e [::assessments/completed? (not completed?)])} (t :completed)]]]))

(defn table-of-contents []
  [:aside.Table-of-contents
   [:ol
    (for [[n section] assessments/section-names]
      [:li {:key (slug :toc n)}
       [:a {:href (str (.-host js/location) "/#" n)
            :on-click (e> (.preventDefault e)
                          (set! (.-hash js/location) n))}
        (t section)]])]])

(defn app []
  (let [progress (<sub [::assessments/progress])]
    [:main
     [:header
      [:div
       [progress-indicator {:width 256} (* progress 100)]
       [introduction]]]
     [search]
     [table-of-contents]
     [assessments/view]]))

(defn ^:dev/after-load main []
  (-> root (.render (reagent/as-element [app]))))

(defn ^:export init []
  (re-frame/dispatch-sync [::assessments/initialize])
  (main))