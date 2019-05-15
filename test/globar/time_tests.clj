(ns globar.time-tests
  (:require [clojure.test :refer :all]
            [globar.calendar.time :as gt]))

(deftest test-merge-chunks
  (let [c1 [120 180]
        c2 [140 150]
        c3 [160 240]
        c4 [220 300]]
    
    (is (= (gt/merge-chunks [c1 c2 c3 c4])
           [[120 300]]))))

