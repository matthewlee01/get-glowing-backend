(ns globar.calendar.calendar-db
  (:require [io.pedestal.log :as log]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [globar.config :as config]
            [globar.db :as db]))

(defn read-vendor-calendar-day
  "this function reads the database to return a map with the following structure:
   {:date string
    :available (collection)
    :booked (collection of time chunks)
    :updated-at string}"
  [vendor-id date]
  (let [raw-result (db/query ["SELECT * FROM vendor_calendar WHERE vendor_id = ? AND date = ?" vendor-id date])
        result (first raw-result)]
    (println result)
    {:date (:date result)
     :available (edn/read-string (:available_edn result))
     :booked (edn/read-string (:booked_edn result))
     :updated-at (:updated_at result)}))                                    
    
(defn upsert-vendor-calendar-day
  "Adds a new calendar day, or changes the value to an existing day if one exists.  
   Existence is determined by looking for the presence of the :updated-at key.
   The format for calendar day is:
       {:date  string
        :available (collection)
        :booked (collection)
        :updated-at string}
   Reads back the record and returns this map.  Note this will need to be a REST
   call as I believe GraphQL expects a nil?"

  [vendor-id calendar-day]
  (log/debug :fn :upsert-calendar-day :vendor vendor-id :calendar-day calendar-day)
  (let [date (:date calendar-day)
        available (:available calendar-day)
        booked (:booked calendar-day)
        updated-at (:update-at calendar-day)]
    
    (if (= updated-at nil)
      (db/execute ["INSERT INTO vendor_calendar (vendor_id, date, available_edn, booked_edn)
                 VALUES (?, ?, ?, ?)" vendor-id date (prn-str available) (prn-str booked)])
      (db/execute ["UPDATE vendor_calendar SET available_edn = ?, booked_edn = ?
                 WHERE vendor_id = ? AND updated_at = ?"] 
                (prn-str available)
                (prn-str booked)
                vendor-id,
                date,
                updated-at)))
  (log/debug :message "upsert-vendor-calendar-day completed")
  (read-vendor-calendar-day vendor-id ( :date calendar-day)))

