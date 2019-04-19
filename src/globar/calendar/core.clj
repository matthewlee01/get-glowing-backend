(ns globar.calendar.core
  (:require [globar.calendar.calendar-db :as cdb]))

(defn read-calendar 
  [vendor-id date]
  (cdb/read-vendor-calendar-day vendor-id date))

(defn write-calendar
  [vendor-id cal-map]
  (let [{:keys [date available booked updated_at]} cal-map]
    (cdb/upsert-vendor-calendar-day vendor-id date available booked updated_at)))
