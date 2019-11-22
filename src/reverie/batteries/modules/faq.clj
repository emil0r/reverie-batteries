(ns reverie.batteries.modules.faq
  (:require [ez-database.core :as db]
            [ez-web.breadcrumbs :refer [crumb]]
            [ez-web.uri :refer [join-uri]]
            [reverie.core :refer [defmodule]]
            [reverie.module.entity :as e]
            [reverie.module :as m]
            [reverie.modules.default :refer [base-link get-display-name pk-cast]]
            [reverie.time :as time]
            [vlad.core :as vlad]))

(defn get-faq-lists [{db :database}]
  (->> {:select [:type :name]
        :from [:batteries_faq_list]
        :order-by [:type]}
       (db/query db)
       (mapv (juxt :name :type))))

(defn view-raw-question [request module {:keys [entity id] :as params} & [errors]]
  (let [entity (m/get-entity module entity)
        id (pk-cast id)
        entity-data (m/get-data module entity params id)
        data (:form-params entity-data)]
    {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                           [(e/slug entity) (e/name entity)]
                           [(str id) (get-display-name entity entity-data)]]))
     :content [:table.table
               (for [[th td] [["Name" :name]
                              ["Date" #(-> % :created (time/format :mysql))]
                              ["Email" :email]
                              ["Phone" :phone]
                              ["Skype" :skype]
                              ["Question" :question]]]
                 [:tr [:th th] [:td (td data)]])]}))

(defmodule reverie.module/faq
  {:name "FAQ"
   :interface? true
   :migration {:path "reverie/batteries/modules/migrations/faq/"
               :automatic? true}
   :template :admin/main
   :entities
   {:raw-question {:name "Raw questions"
                   :table :batteries_faq_raw_question
                   :interface {:disabled #{:add-entity}
                               :display {:name {:name "Name of questioner"
                                                :link? true
                                                :sort :t}
                                         :created {:name "Date"
                                                   :sort :d}}
                               :default-order :name}
                   :fields {}
                   :sections []}
    :list  {:name "List"
            :table :batteries_faq_list
            :interface {:display {:name {:name "Name"
                                         :link? true
                                         :sort :n}
                                  :type {:name "Type"
                                         :sort :t}
                                  :active_p {:name "Active?"
                                             :sort :t}}
                        :default-order :name}
            :fields {:type {:name "Type of FAQ"
                            :type :text
                            :validation (vlad/attr [:type] (vlad/present))}
                     :name {:name "Name"
                            :type :text
                            :validation (vlad/attr [:name] (vlad/present))}
                     :active_p {:name "Active?"
                                :type :boolean}}
            :sections [{:fields [:type :name :active_p]}]}
    :entry {:name "Entry"
            :interface {:display {:question {:name "Question"
                                             :link? true
                                             :sort :q}
                                  :type {:name "Type"
                                         :sort :t}
                                  :ordering {:name "Ordering"
                                             :sort :o}}
                        :default-order :id}
            :table :batteries_faq_entry
            :fields {:question {:name "Question"
                                :type :text
                                :validation (vlad/attr [:question] (vlad/present))}
                     :answer {:name "Answer"
                              :type :textarea
                              :validation (vlad/attr [:answer] (vlad/present))}
                     :type {:name "Who is the question aimed at?"
                            :type :dropdown
                            :options get-faq-lists
                            :initial ""}
                     :ordering {:name "Order"
                                :type :number
                                :cast :int
                                :initial "0"}
                     :visible_p {:name "Visible?"
                                 :type :boolean
                                 :cast :boolean
                                 :initial true}}
            :sections [{:fields [:question :answer :type :ordering :visible_p]}]}}}
  [["/:entity/:id" {:entity #"raw-question" :id #"\d+"} {:any view-raw-question}]])
