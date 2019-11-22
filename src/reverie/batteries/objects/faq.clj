(ns reverie.batteries.objects.faq
  (:require [ez-database.core :as db]
            [reverie.core :refer [defobject defrenderer]]))

(defn get-faq-lists [{db :database}]
  (->> {:select [:type :name]
        :from [:batteries_faq_list]
        :where [:= :active_p true]
        :order-by [:type]}
       (db/query db)
       (mapv (juxt :name :type))))

(defn faq-entry [{:keys [question answer]}]
  [:div.faq-row.separator
   [:div.faq-item
    [:h3.faq-question question]
    [:div.faq-answer answer]]])

(defn get-faq [{{db :database} :reverie :as request} object {:keys [type]} params]
  (let [entries (db/query db {:select [:e.*]
                              :from [[:batteries_faq_entry :e]
                                     [:batteries_faq_list :l]]
                              :where [:and
                                      [:= :e.type :l.type]
                                      [:= :e.type type]
                                      [:= :e.visible_p true]
                                      [:= :l.active_p true]]
                              :order-by [:e.ordering]})]
    {:entries entries}))

(defn faq [{:keys [entries]}]
  (map faq-entry entries))

(defrenderer ::renderer
  {:render-fn :hiccup}
  {:any faq})

(defobject reverie/faq
  {:table "batteries_faq_object"
   :migration {:path "reverie/batteries/objects/migrations/faq/"
               :automatic? true}
   :renderer ::renderer
   :fields {:type {:name "Who is the FAQ adressed to?"
                   :type :dropdown
                   :options get-faq-lists
                   :initial ""}}
   :sections [{:fields [:type]}]}
  {:any get-faq})
