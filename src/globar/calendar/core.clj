(ns globar.calendar.core
  (:require [globar.calendar.calendar-db :as cdb]
            [globar.calendar.time :as ctime]
            [io.pedestal.log :as log]))

(defn valid-time?
  "checks to make sure time makes sense"
  [[hour minute]]
  (if (and (< hour 24)
           (< minute 60)
           (>= hour 0)
           (>= minute 0))
      true
      (do (log/debug :message "invalid time format") false)))

(defn valid-time-chunk?
  "checks each time in a chunk to make sure that it conforms to the format,
  then checks that the beginning is before the end"
  [time-chunk]
  (log/debug :message (str time-chunk))
  (let [time1 (ctime/time-str-to-vec (first time-chunk))
        time2 (ctime/time-str-to-vec (second time-chunk))
        h1 (first time1)
        h2 (first time2)
        m1 (second time1)
        m2 (second time2)]
    (if (and (valid-time? time1)
             (valid-time? time2)
             (<= h1 h2)
             (if (and (= h1 h2) (> m1 m2))
               false
               true))
      (do (log/debug :message "time chunk good") true)
      (do (log/debug :message "time chunk failed") false))))

(defn valid-time-coll?
  "maps over the time-coll to ensure that each time-chunk is correct"
  [time-coll]
  (->> (read-string time-coll)
       (map valid-time-chunk?)
       (every? true?)))

(defn overlapping-bookings?
  "uses merge-chunks to check if the bookings overlap"
  [time-coll]
  (let [merged-coll (ctime/merge-chunks (ctime/string-to-calendar time-coll))]
    (if (= time-coll (ctime/calendar-to-string merged-coll))
      false
      true)))

(defn valid-bookings?
  "checks to ensure that a booking collection has valid times and
  time chunks, and checks for overlap between bookings"
  [time-coll]
  (if (valid-time-coll? time-coll)
    (if (overlapping-bookings? time-coll)
      false
      true)
    (do (log/debug :message "invalid time coll") false)))

(defn read-calendar 
  "reads a vendor's calendar-day from the db"
  [vendor-id date]
  (let [result (cdb/read-calendar-day vendor-id date)
        timestamp (:updated-at result)]
    ;; replace the timestamp with the string equivalent
    (assoc result :updated-at (str timestamp))))

(defn write-calendar
  "upserts a vendor's calendar day with new info"
  [vendor-id cal-map]
  (let [{:keys [date available booked updated-at]} cal-map
        result (if (= nil updated-at)
                 (cdb/insert-calendar-day vendor-id date available booked)
                 ;; if the updated-at field is present, convert it to timestamp
                 (cdb/update-calendar-day vendor-id date available booked
                               (java.sql.Timestamp/valueOf updated-at)))
        timestamp (:updated-at result)]
    ;; replace the timestamp with the string equivalent
    (assoc result :updated-at (str timestamp))))

