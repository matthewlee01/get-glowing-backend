(ns globar.calendar.core
  (:require [globar.calendar.calendar-db :as cdb]
            [globar.calendar.time :as ctime]
            [java-time :as jt]
            [clojure.spec.alpha :as s]))

(def MIN_TIME 0)
(def MAX_TIME 1440) ;;number of minutes in a day
(def DATE_REGEX #"([12]\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01]))") ;;yyyy-MM-dd

(defn get-total-available
  "takes a cal map and returns the sum of the available and template time"
  [cal-map]
  (-> (:available cal-map)
      (concat (:template cal-map))
      (vec)
      (ctime/merge-chunks)))

(defn distinct-time-chunks?
  "uses merge-chunks to check if any time chunks overlap with each other
  within a collection"
  [time-coll]
  (let [merged-coll (ctime/merge-chunks time-coll)]
    (= time-coll merged-coll)))

(defn bookings-available?
  "uses merge-chunks to check that all bookings are within available time"
  [cal-map]
  (let [booked (:booked cal-map)
        total-available (get-total-available cal-map)
        merged-time (ctime/merge-chunks (concat total-available booked))]
    (= merged-time total-available)))

(s/def ::time  ;;describes a valid time value
  (s/and integer?
         #(>= % MIN_TIME)
         #(< % MAX_TIME)))

(s/def ::time-chunk ;;describes a valid time chunk
  (s/and vector?
         #(= (count %) 2)  
         (s/coll-of ::time)  
         #(< (first %) (second %))))  

(s/def ::time-collection ;;describes a valid time collection
  (s/and vector?
         (s/coll-of ::time-chunk))) 

(s/def ::available
  (s/nilable ::time-collection))

(s/def ::template
  (s/nilable ::time-collection))

(s/def ::booked
  (s/nilable (s/and ::time-collection
                    distinct-time-chunks?)))

(s/def ::date
  (s/and string?
         #(re-matches DATE_REGEX %)))
         
(s/def ::valid-calendar
  (s/and (s/keys :req-un [::available ::booked ::template ::date])
         bookings-available?))

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
    (assoc result :updated-at (if timestamp
                                (str timestamp)
                                nil)
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

(defn write-calendar-day
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




