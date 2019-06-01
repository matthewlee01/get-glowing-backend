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

(deftest test-day-of-the-week
  (let [[year1 month1 day1 weekdaynum1 weekdaykey1] [19 4 20 0 :Saturday]
        [year2 month2 day2 weekdaynum2 weekdaykey2] [22 11 11 6 :Friday]]
    (is (= (gt/day-of-the-week year1 month1 day1) weekdaykey1))
    (is (= (gt/day-of-the-week year2 month2 day2) weekdaykey2))))

(deftest test-ymd-conversion
  (let [[timestr1 ymd1] ["2033-11-22" [33 11 22]]
        [timestr2 ymd2] ["2019-05-07" [19 5 7]]
        [timestr3 weekday3] ["2019-4-20" :Saturday]]
    (is (= (vec (gt/datestr->ymd timestr1)) ymd1))
    (is (= (vec (gt/datestr->ymd timestr2)) ymd2))
    (is (= (gt/get-weekday timestr3) weekday3))))
