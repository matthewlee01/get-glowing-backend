(ns globar.calendar_tests
  (:require [clojure.test :refer :all]
            [globar.test_utils :refer [start-test-system! stop-test-system!]]
            [globar.core :as core]
            [globar.calendar.core :as cc]
            [clojure.java.shell :refer [sh]]
            [clojure.edn :as edn]
            [clojure.data.json :as json]))

(deftest test-calendar-read-write
  (start-test-system!)
  (let [vendor-id 1234
        date "01/01/01"
        test-map {:date date
                  :available "((\"02:30\" \"05:00\") (\"23:50\" \"06:15\"))"
                  :booked "((\"07:30\" \"08:00\") (\"13:20\" \"20:45\"))"}
        written-map (cc/write-calendar vendor-id test-map)]
    (let [read-map (cc/read-calendar vendor-id date)]
      (is (= (:date test-map) (:date written-map)))
      (is (= (:available test-map (:available written-map))))
      (is (= (:booked test-map (:booked written-map))))
      (is (= written-map read-map)))))

(deftest test-calendar-calls
  (core/start)
  (let [posted-json (sh "curl" "-H" "Content-Type: application/json" "-d" "{\"date\": \"2019-07-18\", \"available\": \"((12:30 1:30)(4:30 9:30))\", \"booked\":\"((1:15 1:45))\", \"updated-at\":\"2019-04-13 14:23:44\"}" "-X" "POST" "http://localhost:8888/calendar/1234")
        read-json (sh "curl" "http://localhost:8888/calendar/1234/2019-07-18")
        posted-clj (json/read-str (:out posted-json) :key-fn keyword)
        read-clj (json/read-str (:out read-json) :key-fn keyword)]
    (is (= (:date posted-clj) (:date read-clj)))
    (is (= (:booked posted-clj) (:booked read-clj))))
  (core/stop))

(deftest test-calendar-checks
  (let [invalid-time [26 -1]
        invalid-time-chunk '("07:30" "07:20")
        invalid-time-coll "((\"02:30\" \"05:00\") (\"23:50\" \"06:15\") (\"13:20\" \"20:45\"))"
        valid-time [07 45]
        valid-time-chunk '("09:20" "09:50")
        valid-time-coll "((\"02:30\" \"02:55\") (\"03:50\" \"06:15\") (\"13:20\" \"20:45\"))"]
    (is (= (cc/valid-time? (first invalid-time) (second invalid-time)) false))
    (is (= (cc/valid-time? (first valid-time) (second valid-time)) true))
    (is (= (cc/valid-time-chunk? invalid-time-chunk) false))
    (is (= (cc/valid-time-chunk? valid-time-chunk) true))
    (is (= (cc/valid-time-coll? invalid-time-coll) false))
    (is (= (cc/valid-time-coll? valid-time-coll) true))))
