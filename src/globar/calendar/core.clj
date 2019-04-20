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
  [time-coll]
  (->> (read-string time-coll)
       (map valid-time-chunk?)
       (every? true?)))

(defn overlapping-bookings?
  [time-coll]
  (let [merged-coll (ctime/merge-chunks (ctime/string-to-calendar time-coll))]
    (if (= time-coll (ctime/calendar-to-string merged-coll))
      false
      true)))

(defn valid-bookings?
  [time-coll]
  (if (valid-time-coll? time-coll)
    (if (overlapping-bookings? time-coll)
      false
      true)
    (do (log/debug :message "invalid time coll") false)))

(defn read-calendar 
  [vendor-id date]
  (cdb/read-vendor-calendar-day vendor-id date))

(defn write-calendar
  [vendor-id cal-map]
  (let [{:keys [date available booked updated_at]} cal-map]
    (cdb/upsert-vendor-calendar-day vendor-id date available booked updated_at)))

