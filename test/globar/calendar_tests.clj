(ns globar.calendar-tests
  (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system!]]
            [globar.core :as core]
            [globar.calendar.core :as cc]
            [clojure.java.shell :refer [sh]]
            [clojure.edn :as edn]
            [clojure.data.json :as json]))

(use-fixtures :each setup-test-system!)

;; test that we can create a new entry in the calendar table by writing a new
;; calendar entry, reading it back and testing that the contents are the same
;; and then test that we can update the entry by issuing another write
(deftest test-calendar-write-read
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
      (is (= (:success written-map) true))
      (is (= written-map read-map))
      (let [updated-map (assoc read-map :booked "((\"07:30\" \"08:00\") (\"13:20\" \"20:45\") (\"20:45\" \"21:00\"))")
            re-written-map (cc/write-calendar vendor-id updated-map)]
        ;; ignore the success flag (?) and the updated-at timestamp
        (is (= (dissoc updated-map :updated-at)
               (dissoc re-written-map :updated-at))))))
  )

;;"this test creates a row in the calendar table, checks that it was created properly by reading it back.
;; next it will update the row twice.  the first update should succeed and the second should fail with 
;; an error indicating that another operation won the race and the operation needs to be retried"
(deftest test-calendar-calls
  (println "sending")
  ;; first test demonstrates that we can write a new record and read back what we write
  (let [post-result (sh "curl" "-H" "Content-Type: application/json" "-d" "{\"date\": \"2019-07-18\", \"available\": \"((12:30 1:30)(4:30 9:30))\", \"booked\":\"((1:15 1:45))\"}" "-X" "POST" "http://localhost:8889/calendar/1234")
        get-result (sh "curl" "http://localhost:8889/calendar/1234/2019-07-18")
        posted-clj (json/read-str (:out post-result) :key-fn keyword)
        read-clj (json/read-str (:out get-result) :key-fn keyword)]
    (is (= (:date posted-clj) (:date read-clj)))
    (is (= (:booked posted-clj) (:booked read-clj)))
    (is (= (:success posted-clj) true))

    ;; the second test demonstrates that we can update the record by posting a modification
    (let [new-cal (assoc read-clj :booked "((\"12:30\" \"13:00\"))")
          poopy (println "NEW CAL: " new-cal)
          post-result (sh "curl" "-H" 
                          "Content-Type: application/json" 
                          "-d" (json/write-str new-cal) "-X" 
                          "POST" "http://localhost:8889/calendar/1234")
          post-clj (json/read-str (:out post-result) :key-fn keyword)]
      (is (= (:success post-clj) true))
      ;; the last test demonstrates that we can't update without re-fetching to
      ;; avoid update collisions - so recommitting the same record should fail
      (let [post-result (sh "curl" "-H"
                            "Content-Type: application/json" 
                            "-d" (json/write-str new-cal) "-X" 
                            "POST" "http://localhost:8889/calendar/1234")
            post-clj (json/read-str (:out post-result) :key-fn keyword)]
        (is (= (:success post-clj) false))
        (is (= (:error-msg post-clj) "Update collision - please retry the operation")))))
  )

(deftest test-calendar-checks
  (let [invalid-time [26 -1]
        invalid-time-chunk '("07:30" "07:20")
        invalid-time-coll "((\"02:30\" \"05:00\") (\"23:50\" \"06:15\") (\"13:20\" \"20:45\"))"
        valid-time [07 45]
        valid-time-chunk '("09:20" "09:50")
        valid-time-coll "((\"02:30\" \"02:55\") (\"03:50\" \"06:15\") (\"13:20\" \"20:45\"))"
        invalid-bookings "((\"12:30\" \"13:30\") (\"13:00\" \"19:00\"))"
        valid-bookings "((\"12:30\" \"13:30\") (\"14:00\" \"16:00\") (\"17:00\" \"19:00\"))"
        available-sample1 "((\"02:30\" \"03:45\") (\"12:00\" \"16:30\") (\"18:00\" \"21:00\"))"
        available-sample2 "((\"02:30\" \"02:55\") (\"13:20\" \"20:45\"))"
        bookings-sample1 "((\"02:45\" \"03:30\") (\"14:00\" \"16:00\") (\"18:00\" \"19:00\"))"
        bookings-sample2 "((\"02:30\" \"02:50\") (\"14:40\" \"17:30\") (\"19:20\" \"20:45\"))"]
    (is (= (cc/valid-time? invalid-time) false))
    (is (= (cc/valid-time? valid-time) true))
    (is (= (cc/valid-time-chunk? invalid-time-chunk) false))
    (is (= (cc/valid-time-chunk? valid-time-chunk) true))
    (is (= (cc/valid-time-coll? invalid-time-coll) false))
    (is (= (cc/valid-time-coll? valid-time-coll) true))
    (is (= (cc/valid-bookings? invalid-bookings) false))
    (is (= (cc/valid-bookings? valid-bookings) true))
    (is (= (cc/bookings-available? available-sample1 bookings-sample1) true))
    (is (= (cc/bookings-available? available-sample2 bookings-sample1) false))
    (is (= (cc/valid-calendar? available-sample2 bookings-sample2) true))
    (is (= (cc/valid-calendar? available-sample1 invalid-bookings) false))))


