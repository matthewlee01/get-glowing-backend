(ns globar.calendar-db-tests
  (:require [clojure.test :refer :all]
            [globar.calendar.time :as gt]
            [globar.calendar.calendar-db :as db]
            [globar.test_utils :refer [setup-test-system!]]))

(def  t1 [(* 12 60) (* 14 60)])
(def  t2 [(* 17 60) (* 20 60)])
(def  b1 [(* 13 60) (+ 30 (* 13 60))])
(def  b2 [(* 17 60) (+ 45 (* 17 60))])

(def available-times [t1 t2])
(def booked-times [b1 b2])
(def test-date "07-18-2019")

(def expected {:date test-date
               :available available-times
               :booked booked-times
               :success true})

(use-fixtures :each setup-test-system!)

;; write to the db, and then read back and confirm that the values are
;; exactly the same
(deftest test-write-and-read-calendar
  (db/insert-calendar-day 1234 test-date available-times booked-times)
  (let [result (db/read-calendar-day 1234 test-date)]

    ;; confirm that there is an updated at field
    (is (= true (some? (:updated-at result))))

    ;; confirm that the other fields are exactly what we tried to write
    (is (= (dissoc result :updated-at)
           expected))))

;; now figure out if optimistic locking is working at the db level
(deftest test-optimistic-locking
  ;; read a thing from the db - need to write it first so it exists
  (db/insert-calendar-day 1234 test-date available-times booked-times)
  (let [result (db/read-calendar-day 1234 test-date)
        updated-at (:updated-at result)

        ;; first write a new value to the db by updating the booked value
        ;; and writing back to the db
        first-result (db/update-calendar-day 1234
                                           test-date
                                           available-times
                                           [t1 t2 b1 b2]    ;; value is not important here
                                           updated-at)]
    (is (= true (:success first-result)))

       ;; now try to do the same operation again - with the original timestamp
       ;; and expect it to fail now
    (let [second-result (db/update-calendar-day 1234
                                                test-date
                                                available-times
                                                [t1 t2 b1 b2]
                                                updated-at)]
      (is (= false (:success second-result)))
      (is (= (str "Update collision - please retry the operation")
             (:error-msg second-result))))))

