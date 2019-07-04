(ns globar.calendar.core
  (:require [globar.calendar.calendar-db :as cdb]
            [globar.calendar.time :as ctime]
            [java-time :as jt]))

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

(defn get-template
  [vendor-id]
  (cdb/read-vendor-template vendor-id))

(defn write-template
  [vendor-id new-template]
  (cdb/update-template vendor-id new-template))

(defn read-calendar-day
  "reads a vendor's calendar-day from the db"
  [vendor-id date]
  (let [result (cdb/read-calendar-day vendor-id date)
        timestamp (:updated-at result)
        weekday (ctime/get-weekday date)
        template (weekday (get-template vendor-id))]
    ;; replace the timestamp with the string equivalent
    (assoc result :updated-at timestamp
                  :template template)))

(defn day-before
  [date]
  (let [java-day-before (jt/minus (jt/local-date date) (jt/days 1))]
    (jt/format "yyyy-MM-dd" java-day-before)))

(defn day-after
  [date]
  (let [java-day-after (jt/plus (jt/local-date date) (jt/days 1))]
    (jt/format "yyyy-MM-dd" java-day-after)))

(defn insert-booking
  [cal-map new-booking]
  (let [updated-bookings (-> (:booked cal-map)
                             (conj new-booking))]
    (->> updated-bookings
         (sort-by first)
         (vec)
         (assoc cal-map :booked))))

(defn get-total-available
  [cal-map]
  (-> (:available cal-map)
      (concat (:template cal-map))
      (vec)
      (ctime/merge-chunks)))

(defn read-calendar
  "reads 3 calendar days from the db"
  [vendor-id date]
  (let [date-before (day-before date)
        date-after (day-after date)
        cal-before (read-calendar-day vendor-id date-before)
        cal-day (read-calendar-day vendor-id date)
        cal-after (read-calendar-day vendor-id date-after)]
    {:day-before {:date date-before
                  :calendar cal-before}
     :day-of {:date date
              :calendar cal-day}
     :day-after {:date date-after
                 :calendar cal-after}}))

(defn write-calendar
  "upserts a vendor's calendar day with new info"
  [vendor-id cal-map]
  (let [{:keys [date available booked updated-at]} cal-map
        result (if (= nil updated-at)
                 (cdb/insert-calendar-day vendor-id date available booked)
                 ;; if the updated-at field is present, convert it to timestamp
                 (cdb/update-calendar-day vendor-id date available booked
                               (java.sql.Timestamp/valueOf (str updated-at))))
        timestamp (:updated-at result)]
    ;; replace the timestamp with the string equivalent
    (assoc result :updated-at (str timestamp))))




