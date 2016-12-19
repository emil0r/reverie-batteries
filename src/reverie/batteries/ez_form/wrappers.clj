(ns reverie.batteries.ez-form.wrappers
  (:require [clojure.zip :as zip]
            [ez-form.decorate :refer [decor]]
            [reverie.i18n :refer [t]]))



(defmethod decor :?t [form loc]
  (let [k (zip/node (zip/next loc))]
    (-> loc
        (zip/remove)
        (zip/right)
        (zip/replace (t k)))))
