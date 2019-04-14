(ns globar.time
  (:require [java-time :as jt]))

(def LESS_THAN -1)

;;(def c1 {:start (jt/local-time 12 30) :end (jt/local-time 13 30)})
;;(def c2 {:start (jt/local-time 12 45) :end (jt/local-time 13 15)})
;;(def c3 {:start (jt/local-time 12 45) :end (jt/local-time 13 45)})
;;(def c4 {:start (jt/local-time 12 15) :end (jt/local-time 13 45)})
(def c1 {:start 1230 :end 1330})
(def c2 {:start 1245 :end 1315})
(def c3 {:start 1245 :end 1345})
(def c4 {:start 1215 :end 1345})


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

(println (merge-chunks [c1 c2 c3 c4]))
