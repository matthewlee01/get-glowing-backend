(ns globar.calendar.time
  (:require [java-time :as jt]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def LESS_THAN -1)
(def COLON_INDEX 2)
(def CENTURY 20)
(def WEEKDAYNUM->KEY {0 :Saturday
                      1 :Sunday
                      2 :Monday
                      3 :Tueday
                      4 :Wednesday
                      5 :Thursday
                      6 :Friday})

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

(defn datestr->ymd [datestring]
  "takes string with format YYYY-MM-DD and returns (yy mm dd) as an array of integers"
  (->> (str/split (subs datestring 2) #"(-)")
       (map #(Integer/parseInt %))))

(defn day-of-the-week [year month day]
  "calculates the day of the week using a magic formula, lots of magic numbers and operations. 
  kevin understands it."
  (let [[year month] (if (< month 3)
                         [(- year 1) (+ month 12)]
                         [year month])]
    (WEEKDAYNUM->KEY
      (mod (+ day
              (int (* 13 (+ month 1) 0.2))
              (* 5 CENTURY)
              year
              (int (/ year 4))
              (int (/ CENTURY 4))) 7))))

(defn get-weekday [datestring]
  "uses datestr->ymd and day-of-week to directly get the day of week key from a datestring"
  (->> datestring
       (datestr->ymd)
       (apply day-of-the-week)))
