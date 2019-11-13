(ns batteries.help.menu-test
  (:require [clojure.test :refer :all]
            [reverie.batteries.helpers.menu :refer [assemble-tree]]))

(deftest assemble-menu-test
  (testing "assemble-menu"
    (let [pages [{:serial 1} {:parent 1 :serial 2} {:parent 1 :serial 3} {:parent 2 :serial 4} {:parent 3 :serial 5} {:parent 3 :serial 6}]]
      (is (= (assemble-tree pages)
             {:serial 1 :children [{:parent 1 :serial 2
                                    :children [{:parent 2 :serial 4}]}
                                   {:parent 1 :serial 3
                                    :children [{:parent 3 :serial 5}
                                               {:parent 3 :serial 6}]}]})))))
