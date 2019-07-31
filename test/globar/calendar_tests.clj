(ns globar.calendar-tests
  (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system!]]
            [globar.core :as core]
            [globar.calendar.core :as c-c]
            [clojure.java.shell :refer [sh]]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [globar.rest-api :as ra]
            [globar.bookings.core :as b-c]
            [globar.calendar.error-parsing :as c-ep]
            [globar.error-parsing :as ep]
            [clojure.spec.alpha :as s]))

(use-fixtures :each setup-test-system!)

;; test that we can create a new entry in the calendar table by writing a new
;; calendar entry, reading it back and testing that the contents are the same
;; and then test that we can update the entry by issuing another write
(deftest test-calendar-write-read
  (let [vendor-id 1234
        date "2001-01-01"
        test-map {:date date
                  :available "[[60 180] [240 360]]"
                  :booked "[[60 120] [300 360]]"}
        written-map (c-c/write-calendar-day vendor-id test-map)]
    (let [read-map (c-c/read-calendar-day vendor-id date)]
      (is (= (:date test-map) (:date written-map)))
      (is (= (:available test-map (:available written-map))))
      (is (= (:booked test-map (:booked written-map))))
      (is (= (:success written-map) true))
      (is (= (dissoc written-map :updated-at :template)
             (dissoc read-map :updated-at :template)))
      (let [updated-map (assoc read-map :booked "[[0 60] [240 300] [600 660]]")
            re-written-map (c-c/write-calendar-day vendor-id updated-map)]
        ;; ignore the success flag (?) and the updated-at timestamp
        (is (= (dissoc updated-map :updated-at :template)
               (dissoc re-written-map :updated-at :template)))))))


;;"this test creates a row in the calendar table, checks that it was created properly by reading it back.
;; next it will update the row twice.  the first update should succeed and the second should fail with 
;; an error indicating that another operation won the race and the operation needs to be retried"
(deftest test-calendar-calls
  ;; first test demonstrates that we can write a new record and read back what we write
  (let [post-result (sh "curl" "-H" "Content-Type: application/json" "-d" "{\"date\": \"2019-07-18\", \"available\": \"[[240 360] [480 540] [600 720]]\", \"booked\":\"[[300 360]]\"}" "-X" "POST" "http://localhost:8889/calendar/1234")
        ;; now read the calendar back
        get-result (sh "curl" "http://localhost:8889/calendar/1234/2019-07-18")
        ;; convert json to clojure data
        posted-clj (json/read-str (:out post-result) :key-fn keyword)
        read-clj (json/read-str (:out get-result) :key-fn keyword)
        ;; we just want the day in question, not the two adjacent days
        read-cal (get-in read-clj [:day-of :calendar])]
    (println "read after write: " read-cal)
    (is (= (:date posted-clj) (:date read-cal)))
    (is (= (:booked posted-clj) (:booked read-cal)))
    (is (= (:success posted-clj) true))

    ;; the second test demonstrates that we can update the record by posting a modification
    (let [new-cal (assoc read-cal :booked "[[720 780]]")
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


(deftest test-booking-template
  (let [request {:json-params {:time [360 419]
                               :vendor-id 1234
                               :date "2019-07-25"
                               :service 123400
                               :user-id 234}}]
    (is (= (:status (b-c/upsert-booking request)) 200))))
                               
(deftest test-calendar-checks
  (let [invalid-time -60                                ;; time cannot be negative 
        invalid-time-chunk [660 240]                    ;; first time must be before second time 
        invalid-time-coll [[0 120] [420 300] [720 840]] ;; coll contains invalid time chunk
        valid-time 300
        valid-time-chunk [480 540]
        valid-time-coll [[60 180] [300 360] [600 660]]
        overlapping-bookings [[60 180] [120 240]]
        good-calendar {:available [[60 240] [300 600]]
                       :booked [[60 120] [540 660]]
                       :template [[300 660]]
                       :date "2020-01-03"}
        bad-calendar {:available [[3 [5 89]] ["3" "9"] [12]]
                      :booked overlapping-bookings
                      :template 2345
                      :date "12-12-2012"}]        
    (is (= (s/valid? ::c-c/time invalid-time) false))        
    (is (= (s/valid? ::c-c/time valid-time) true))
    (is (= (s/valid? ::c-c/time-chunk invalid-time-chunk) false))
    (is (= (s/valid? ::c-c/time-chunk valid-time-chunk) true))
    (is (= (s/valid? ::c-c/time-collection invalid-time-coll) false))
    (is (= (s/valid? ::c-c/time-collection valid-time-coll) true))
    (is (= (s/valid? ::c-c/valid-calendar good-calendar) true))
    (is (= (s/valid? ::c-c/valid-calendar bad-calendar) false))
    (is (= (s/valid? ::c-c/valid-calendar "i am a monkey man. they call me mr monkey man.") false))
    (let [bad-bookings-cal (assoc good-calendar :booked overlapping-bookings)
          error-msg "Sorry, that time is already booked. Please try a different time."
          error-code :101
          spec-error (s/explain-str ::c-c/valid-calendar bad-bookings-cal)
          generated-error-data (ep/get-error-data ep/ERROR_MSG_SET_EN c-ep/get-error-code spec-error c-ep/ERROR_CODE_KEY)]
      (is (= error-msg (:message generated-error-data)))
      (is (= error-code (:code generated-error-data))))
    (let [incomplete-cal (dissoc good-calendar :template)
          bad-date-cal (assoc good-calendar :date "12/12/2012")
          unavailable-cal (assoc good-calendar :available [[0 60]])
          malformed-cal "i am not a calendar. i am a monkey man."
          bad-time-cal (assoc good-calendar :booked [[-23 120]])
          bad-time-chunk-cal (assoc good-calendar :template [[600 500]])]
      (is (= (c-ep/get-error-code (s/explain-str ::c-c/valid-calendar incomplete-cal)) :201))
      (is (= (c-ep/get-error-code (s/explain-str ::c-c/valid-calendar bad-date-cal)) :102))
      (is (= (c-ep/get-error-code (s/explain-str ::c-c/valid-calendar unavailable-cal)) :100))
      (is (= (c-ep/get-error-code (s/explain-str ::c-c/valid-calendar malformed-cal)) :200))
      (is (= (c-ep/get-error-code (s/explain-str ::c-c/valid-calendar bad-time-cal)) :212))
      (is (= (c-ep/get-error-code (s/explain-str ::c-c/valid-calendar bad-time-chunk-cal)) :209)))))
          
(deftest test-templates
 (let [vendor-id 1236
       test-template {:Sunday [[100 200] [500 600]]
                      :Tuesday [[600 700] [1000 1300]]}
       written-template (c-c/write-template vendor-id test-template)
       read-template (c-c/get-template vendor-id)]
    (is (= written-template read-template))))
   
