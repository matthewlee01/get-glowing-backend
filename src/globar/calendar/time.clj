(ns globar.calendar.time
  (:require [java-time :as jt]))

(def LESS_THAN -1)

(defn squish
  "this reduction function takes a collection representing a stack and
  next time chunk to be processed.  if the accumulated collection is
  empty, just add the interval.  if the top of the stack and the time
  chunk don't overlap, just push both back onto the stack.  if there is
  overlap then replace the top of the stack with a merged chunk"
  [coll interval-2]
  (if (= 0 (count coll))
    [interval-2]
    (let [interval-1 (peek coll)
          new-coll (pop coll)]
      (if (= LESS_THAN (compare (:end interval-1) (:start interval-2)))
        (conj new-coll interval-1 interval-2)
        (if (= LESS_THAN (compare (:end interval-1) (:end interval-2)))
          (conj new-coll {:start (:start interval-1) :end (:end interval-2)})
          (conj new-coll interval-1))))))

(defn merge-chunks
  "this function takes an unordered collection of time chunks
   and returns a collection of chunks representing a merge of
   all overlapping elements of the input collection"
  [chunk-coll]
  (let [sorted-chunks (sort-by :start chunk-coll)]
    (reduce squish [] sorted-chunks)))


(defn nonzero-duration? [[x y]]
  (not= x y))

(defn net-time
  "takes two collections, the first representing available time, the second 
   representing booked time, and returns a collection representing the 
   net available time.  there should never be booked time that doesn't overlap
   with available time."
  [avail booked]
  (->> (concat (reduce concat avail) (reduce concat booked)) ; flatten into one giant seq
       (sort)
       (partition 2)
       (filter nonzero-duration?)))

(defn calendar-to-string
  "this function takes a collection of time chunks and returns a string that
   represents the collection to be stored in the db"
  [coll])

(defn string-to-calendar
  "this function reverses calendar-to-string and takes a string from the db
   and reconstructs the collection of time chunks"
  [coll-str])


