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
                  :available "[[60 180] [240 360]]"
                  :booked "[[60 120] [300 360]]"}
        written-map (cc/write-calendar vendor-id test-map)]
    (let [read-map (cc/read-calendar vendor-id date)]
      (is (= (:date test-map) (:date written-map)))
      (is (= (:available test-map (:available written-map))))
      (is (= (:booked test-map (:booked written-map))))
      (is (= (:success written-map) true))
      (is (= (dissoc written-map :updated-at :template)
             (dissoc read-map :updated-at :template)))
      (let [updated-map (assoc read-map :booked "[[0 60] [240 300] [600 660]]")
            re-written-map (cc/write-calendar vendor-id updated-map)]
        ;; ignore the success flag (?) and the updated-at timestamp
        (is (= (dissoc updated-map :updated-at :template)
               (dissoc re-written-map :updated-at :template)))))))


;;"this test creates a row in the calendar table, checks that it was created properly by reading it back.
;; next it will update the row twice.  the first update should succeed and the second should fail with 
;; an error indicating that another operation won the race and the operation needs to be retried"
(deftest test-calendar-calls
  (println "sending")
  ;; first test demonstrates that we can write a new record and read back what we write
  (let [post-result (sh "curl" "-H" "Content-Type: application/json" "-d" "{\"date\": \"2019-07-18\", \"available\": \"[[240 360] [480 540] [600 720]]\", \"booked\":\"[[300 360]]\"}" "-X" "POST" "http://localhost:8889/calendar/1234")
        get-result (sh "curl" "http://localhost:8889/calendar/1234/2019-07-18")
        posted-clj (json/read-str (:out post-result) :key-fn keyword)
        read-clj (json/read-str (:out get-result) :key-fn keyword)]
    (is (= (:date posted-clj) (:date read-clj)))
    (is (= (:booked posted-clj) (:booked read-clj)))
    (is (= (:success posted-clj) true))

    ;; the second test demonstrates that we can update the record by posting a modification
    (let [new-cal (assoc read-clj :booked "[[720 780]]")
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
        (is (= (:error-msg post-clj) "Update collision - please retry the operation"))))))


(deftest test-calendar-checks
  (let [invalid-time -60                                ;; time cannot be negative 
        invalid-time-chunk [660 240]                    ;; first time must be before second time 
        invalid-time-coll [[0 120] [420 300] [720 840]] ;; coll contains invalid time chunk
        valid-time 300
        valid-time-chunk [480 540]
        valid-time-coll [[60 180] [300 360] [600 660]]
        invalid-bookings [[60 180] [120 240]]           ;; bookings cannot overlap 
        valid-bookings [[120 180] [240 360] [780 840]]
        available-sample1 [[120 600] [720 840]]
        available-sample2 [[0 60] [300 420] [840 900]]
        bookings-sample1 [[300 360] [720 780]]
        bookings-sample2 [[0 60] [360 420]]]
    (is (= (cc/valid-time? invalid-time) false))        
    (is (= (cc/valid-time? valid-time) true))
    (is (= (cc/valid-time-chunk? invalid-time-chunk) false))
    (is (= (cc/valid-time-chunk? valid-time-chunk) true))
    (is (= (cc/valid-time-coll? invalid-time-coll) false))
    (is (= (cc/valid-time-coll? valid-time-coll) true))
    (is (= (cc/bookings-available? available-sample1 bookings-sample1) true))  
    (is (= (cc/bookings-available? available-sample2 bookings-sample1) false)) ;; bookings must be fully within available time
    (is (= (cc/valid-calendar? available-sample2 bookings-sample2) true))
    (is (= (cc/valid-calendar? available-sample1 invalid-bookings) false))))   ;; this calendar contains an invalid booking


(deftest test-templates
 (let [vendor-id 1236
       test-template {:Sunday [[100 200] [500 600]]
                      :Tuesday [[600 700] [1000 1300]]}
       written-template (cc/write-template vendor-id test-template)
       read-template (cc/get-template vendor-id)]
    (is (= written-template read-template))))
   
