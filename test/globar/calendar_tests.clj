(ns globar.calendar_tests
  (:require [clojure.test :refer :all]
            [globar.test_utils :refer [start-test-system! stop-test-system!]]
            [globar.calendar.core :as core]))

(deftest test-calendar-read-write
  (start-test-system!)
  (let [vendor-id 1234
        date "01/01/01"
        test-map {:date date
                  :available "((\"02:30\" \"05:00\") (\"23:50\" \"06:15\"))"
                  :booked "((\"07:30\" \"08:00\") (\"13:20\" \"20:45\"))"}
        written-map (core/write-calendar vendor-id test-map)]
    (let [read-map (core/read-calendar vendor-id date)]
      (is (= (:date test-map) (:date written-map)))
      (is (= (:available test-map (:available written-map))))
      (is (= (:booked test-map (:booked written-map))))
      (is (= written-map read-map))))
  (stop-test-system!))




