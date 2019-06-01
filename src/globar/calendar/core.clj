(ns globar.calendar.core
  (:require [clojure.edn :as edn]
            [globar.calendar.calendar-db :as cdb]
            [globar.calendar.time :as ctime]
            [io.pedestal.log :as log]))

(defn valid-time?
  "checks to make sure time is within the correct range"
  [time]
  (and (>= time 0)
       (< time 1440))) ;; number of minutes in a day

(defn valid-time-chunk?
  "checks each time in a chunk to make sure that it conforms to the format,
  then checks that the beginning is before the end"
  [[start-time end-time]]
  (and (valid-time? start-time)
       (valid-time? end-time)
       (< start-time end-time)))

(defn valid-time-coll?
  "maps over a time-coll to ensure that each time-chunk is correct"
  [time-coll]
  (->> (map valid-time-chunk? time-coll)
       (every? true?)))

(defn overlapping-chunks?
  "uses merge-chunks to check if any time chunks overlap with each other
  within a collection"
  [time-coll]
  (let [merged-coll (ctime/merge-chunks time-coll)]
    (not (= time-coll merged-coll))))

(defn bookings-available?
  "uses merge-chunks to check that all bookings are within available time"
  [available-time booked-time]
  (let [merged-calendars (ctime/merge-chunks (concat available-time booked-time))]
    (= available-time merged-calendars)))

(defn valid-calendar?
  "checks bookings & available collections for formatting & overlap"
  [available-time-coll bookings-time-coll]
  (and (valid-time-coll? available-time-coll)
       (valid-time-coll? bookings-time-coll)
       (not (overlapping-chunks? bookings-time-coll))
       (bookings-available? available-time-coll bookings-time-coll)))

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

(defn get-template
  [vendor-id]
  (cdb/read-vendor-template vendor-id))

(defn write-template
  [vendor-id new-template]
  (cdb/update-template vendor-id new-template))





