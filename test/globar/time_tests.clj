(ns globar.time-tests
  (:require [clojure.test :refer :all]
            [globar.calendar.time :as gt]
            [java-time :as jt]))

(deftest test-merge-chunks
  (let [c1 [(jt/local-time 12 30) (jt/local-time 13 30)]
        c2 [(jt/local-time 12 45) (jt/local-time 13 15)]
        c3 [(jt/local-time 12 45) (jt/local-time 13 45)]
        c4 [(jt/local-time 12 15) (jt/local-time 13 45)]]
    
    (is (= (gt/merge-chunks [c1 c2 c3 c4])
           [[(jt/local-time 12 15) (jt/local-time 13 45)]]))))

(def t1 [(jt/local-time 12 30) (jt/local-time 13 30)])
(def t2 [(jt/local-time 14 00) (jt/local-time 19 00)])
(def t3 [(jt/local-time 00 00) (jt/local-time 04 35)])
(def t4 [(jt/local-time 11 00) (jt/local-time 17 50)])
(def b1 [(jt/local-time 12 45) (jt/local-time 12 55)])
(def b2 [(jt/local-time 13 15) (jt/local-time 13 30)])

(def available-times [t1 t2])
(def available-times2 [t3 t4 t1])

(def booked-times [b1 b2])

(deftest test-net-time
  (is (= (gt/net-time available-times booked-times)
         `((~(jt/local-time 12 30) ~(jt/local-time 12 45))
           (~(jt/local-time 12 55) ~(jt/local-time 13 15))
           (~(jt/local-time 14 00) ~(jt/local-time 19 00))))))

(deftest test-calendar-conversions
  (let [test-string "((\"12:30\" \"13:30\") (\"14:00\" \"19:00\"))"
        test-string2 "((\"00:00\" \"04:35\") (\"11:00\" \"17:50\") (\"12:30\" \"13:30\"))"]
    (is (= (gt/calendar-to-string available-times) test-string))
    (is (= (gt/string-to-calendar test-string) available-times))
    (is (= (gt/string-to-calendar test-string2) available-times2))
    (is (= (gt/calendar-to-string available-times2) test-string2))
    (is (= (gt/calendar-to-string '()) "()"))
    (is (= (gt/string-to-calendar "") '()))
    (is (= (gt/calendar-to-string available-times) (-> available-times
                                                       (gt/calendar-to-string)
                                                       (gt/string-to-calendar)
                                                       (gt/calendar-to-string))))
    (is (= (gt/string-to-calendar test-string2) (-> test-string2
                                                    (gt/string-to-calendar)
                                                    (gt/calendar-to-string)
                                                    (gt/string-to-calendar))))))

