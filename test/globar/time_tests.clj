(ns globar.time-tests
  (:require [clojure.test :refer :all]
            [globar.calendar.time :as gt]
            [java-time :as jt]))

(deftest test-merge-chunks
  (let [c1 {:start (jt/local-time 12 30) :end (jt/local-time 13 30)}
        c2 {:start (jt/local-time 12 45) :end (jt/local-time 13 15)}
        c3 {:start (jt/local-time 12 45) :end (jt/local-time 13 45)}
        c4 {:start (jt/local-time 12 15) :end (jt/local-time 13 45)}]
    
    (is (= (gt/merge-chunks [c1 c2 c3 c4])
           [{:start (jt/local-time 12 15) :end (jt/local-time 13 45)}]))))

(def t1 [(jt/local-time 12 30) (jt/local-time 13 30)])
(def t2 [(jt/local-time 14 00) (jt/local-time 19 00)])
(def b1 [(jt/local-time 12 45) (jt/local-time 12 55)])
(def b2 [(jt/local-time 13 15) (jt/local-time 13 30)])

(def available-times [t1 t2])
(def booked-times [b1 b2])

(deftest test-net-time
  (is (= (gt/net-time available-times booked-times)
         `((~(jt/local-time 12 30) ~(jt/local-time 12 45))
           (~(jt/local-time 12 55) ~(jt/local-time 13 15))
           (~(jt/local-time 14 00) ~(jt/local-time 19 00))))))

(deftest test-calendar-conversions
  (let [test-string "((\"12:30\" \"13:30\")(\"14:00\" \"19:00\"))"]
    (is (= (gt/calendar-to-string available-times) test-string))
    (is (= (gt/string-to-calendar test-string) available-times))))
