(ns globar.calendar.time
  (:require [java-time :as jt]
            [clojure.edn :as edn]))

(def LESS_THAN -1)
(def COLON_INDEX 2)

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
      (if (= LESS_THAN (compare (second interval-1) (first interval-2)))
        (conj new-coll interval-1 interval-2)
        (if (= LESS_THAN (compare (second interval-1) (second interval-2)))
          (conj new-coll [(first interval-1) (second interval-2)])
          (conj new-coll interval-1))))))

(defn merge-chunks
  "this function takes an unordered collection of time chunks
   and returns a collection of chunks representing a merge of
   all overlapping elements of the input collection"
  [chunk-coll]
  (let [sorted-chunks (sort-by first chunk-coll)]
    (reduce squish [] sorted-chunks)))


(defn nonzero-duration? [[x y]]
  (not= x y))


