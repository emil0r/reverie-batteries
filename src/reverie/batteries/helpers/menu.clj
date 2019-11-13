(ns reverie.batteries.helpers.menu
  (:require [clojure.zip :as zip]
            [ez-database.core :as db]
            [reverie.page :as page]
            [yesql.core :refer [defqueries]]))


(defqueries "reverie/batteries/helpers/menu.sql")

(defn assemble-tree [coll]
  (let [root (first coll)
        by-parent (group-by :parent coll)]
    (loop [tree (zip/zipper some?
                            #(by-parent (:serial %))
                            #(assoc %1 :children %2)
                            (first (by-parent (:parent root))))]
      (if (zip/end? tree)
        (zip/root tree)
        (recur (zip/next (zip/edit tree identity)))))))

(defn get-menu-pages
  ([db] (get-menu-pages db nil))
  ([db
    {:keys [level status published? root]
     :or {level 1
          root 1
          published? true
          status :visible}
     :as opts}]
   (let [query (case status
                 :hidden sql-get-menu-pages-visibility-hidden
                 :either sql-get-menu-pages-visibility-either
                 sql-get-menu-pages-visibility-visible)
         version (if published? 1 0)]
     (->> {:level level :version version :root root}
          (db/query db query)
          (map page/page)
          (assemble-tree)))))
