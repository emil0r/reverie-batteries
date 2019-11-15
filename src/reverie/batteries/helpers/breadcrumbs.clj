(ns reverie.batteries.helpers.breadcrumbs
  (:require [ez-database.core :as db]
            [reverie.page :as page]
            [yesql.core :refer [defqueries]]))

(defqueries "reverie/batteries/helpers/breadcrumbs.sql")


(defn get-breadcrumbs
  ([db page] (get-breadcrumbs nil db page))
  ([{:keys [published?] :or {published? true}} db page]
   (let [serial (page/serial page)
         version (if published? 1 0)]
     (->> {:serial serial
           :version version}
          (db/query db sql-get-breadcrumbs)
          (map page/page)))))
