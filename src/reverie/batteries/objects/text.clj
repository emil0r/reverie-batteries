(ns reverie.batteries.objects.text
  (:require [reverie.core :refer [defobject]]))

(defn- text [request object {:keys [text]} params]
  text)

(defobject reverie/text
  {:table "batteries_text"
   :migration {:path "reverie/batteries/objects/migrations/text/"
               :automatic? true}
   :fields {:text {:initial ""
                   :type :richtext
                   :name "Text"}}
   :sections [{:fields [:text]}]}
  {:any text})
