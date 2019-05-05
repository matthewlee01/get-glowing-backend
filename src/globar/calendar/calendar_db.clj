(ns globar.calendar.calendar-db
  (:require [io.pedestal.log :as log]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [globar.config :as config]
            [globar.db :as db]))

(defn read-calendar-day
  "this function reads the database to return a map with the following structure:
   {:date - string
    :available - a collection of time chunks 
    :booked - a collection of time chunks 
    :updated-at - string}"
  [vendor-id date]

  (try
    (let [raw-result (db/query ["SELECT * FROM vendor_calendar WHERE vendor_id = ? AND date = ?" vendor-id date])
          result (first raw-result)
          available (edn/read-string (:available_edn result))
          booked (edn/read-string (:booked_edn result))]
      (println result)
      {:date (:date result)
       :available available
       :booked booked
       :updated-at (:updated_at result)
       :success true})
    (catch Exception e
      (log/error :DB-ERROR (prn-str e))
      {:success false
       :error-msg "An error ocurred while trying to read a calendar from the database."})))

(defn insert-calendar-day
  "this function inserts a new row into the vendor_calendar table and returns the most recent values.
   Notably it will return a value for updated-at, which is necessary to do further updates.  If an
   error is encountered, the returned hash map will contain :success false and a :error-msg key"
  [vendor-id date available booked]
  (log/debug :fn :insert-vendor-calendar-day :vendor vendor-id :date date
             :available available :booked booked)
  (try
    (let [available-edn (prn-str available)
          booked-edn (prn-str booked)
          result (db/execute ["INSERT INTO vendor_calendar (vendor_id, date, available_edn, booked_edn)
                               VALUES (?, ?, ?, ?)" vendor-id date available-edn booked-edn])
          success? (= 1 (first result))
          latest (read-calendar-day vendor-id date)]

      (if success?
        (merge latest {:success true})
        (do
          (log/debug :fn :insert-vendor-calendar-day :error "unexpected result of 0 rows updated when trying to insert a new calendar entry.")
          (merge latest {:success false
                         :error-msg "Something unexpected happened while trying to add new calendar information to the system."}))))

    (catch Exception e
      (log/error :DB-ERROR (prn-str e))
      (let [latest (read-calendar-day vendor-id date)]
        (merge latest {:success false
                       :error-msg "An error occurred when trying to save the calendar information."})))))
    
(defn update-calendar-day
  [vendor-id date available booked updated-at]
  (log/debug :fn :update-vendor-calendar-day :vendor vendor-id :date date
             :available available :booked booked :updated-at updated-at)
  (try
    (let [available-edn (prn-str available)
          booked-edn (prn-str booked)
          result (db/execute ["UPDATE vendor_calendar SET available_edn = ?, booked_edn = ?
                              WHERE vendor_id = ? AND date = ? AND updated_at = ?" 
                              available-edn
                              booked-edn
                              vendor-id
                              date
                              updated-at])
          success? (= 1 (first result))
          latest (read-calendar-day vendor-id date)]
  
      (if success?
        (merge latest {:success true})
        (do
          (log/debug :fn :update-vendor-calendar-day :info "optimistic locking has caused an update to fail.")
          (merge latest {:success false
                         :error-msg "Update collision - please retry the operation"}))))
    (catch Exception e
      (log/error :DB-ERROR e)
      {:success false
       :error-msg "An error occurred while trying to update the calendar for this date.  Please contact support for help."})))

