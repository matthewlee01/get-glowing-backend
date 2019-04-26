(ns globar.calendar.calendar-db
  (:require [io.pedestal.log :as log]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [globar.config :as config]
            [globar.db :as db]))

(defn read-calendar-day
  "this function reads the database to return a map with the following structure:
   {:date string
    :available (string representing time chunks)
    :booked (string representing time chunks)
    :updated-at string}"
  [vendor-id date]
  (let [raw-result (db/query ["SELECT * FROM vendor_calendar WHERE vendor_id = ? AND date = ?" vendor-id date])
        result (first raw-result)]
    (println result)
    {:date (:date result)
     :available (:available_edn result)
     :booked (:booked_edn result)
     :updated-at (:updated_at result)}))                                    

(defn insert-calendar-day
  "this function inserts a new row into the vendor_calendar table and returns the most recent values.
   Notably it will return a value for updated-at, which is necessary to do further updates.  If an
   error is encountered, the returned hash map will contain a :error key"
  [vendor-id date available booked]
  (log/debug :fn :insert-vendor-calendar-day :vendor vendor-id :date date
             :available available :booked booked)
  (try
    (let [result (db/execute ["INSERT INTO vendor_calendar (vendor_id, date, available_edn, booked_edn)
                               VALUES (?, ?, ?, ?)" vendor-id date available booked])
          success? (= 1 (first result))
          fresh-data (read-calendar-day vendor-id date)]

      (when (not success?)
        (log/debug :fn :insert-vendor-calendar-day :error "unexpected result of 0 rows updated when trying to insert a new calendar entry."))
          
      (if success?
        fresh-data
        (assoc fresh-data :error {:error "Something unexpected happened while trying to add new calendar information to the system."})))

    (catch Exception e
      (log/error :DB-ERROR (prn-str e))
      (let [fresh-data (read-calendar-day vendor-id date)]
        (assoc fresh-data :error {:error "An error occurred when trying to save the calendar information."})))))
    
(defn update-calendar-day
  [vendor-id date available booked updated-at]
  (log/debug :fn :update-vendor-calendar-day :vendor vendor-id :date date
             :available available :booked booked :updated-at updated-at)
  (try
    (let [result (db/execute ["UPDATE vendor_calendar SET available_edn = ?, booked_edn = ?
                             WHERE vendor_id = ? AND date = ? AND updated_at = ?" 
                available
                booked
                vendor-id,
                date
                updated-at])]
  
      (if (= 0 (first result))
        (do
          (log/debug :fn :update-vendor-calendar-day :info "optimistic locking has caused an update to fail.")
          {:error "Update collision - please retry the operation"})
        {:success true}))
    (catch Exception e
      (log/error :DB-ERROR e)
      {:error "An error occurred while trying to update the calendar for this date.  Please contact support for help."})))

(defn upsert-calendar-day
  "Adds a new calendar day, or changes the value to an existing day if one exists.  
   Existence is determined by looking for the presence of the :updated-at key.
   The format for calendar day is:
       {:date  string
        :available string
        :booked string
        :updated-at string}
   Reads back the record and returns this map.  Note this will need to be a REST
   call as I believe GraphQL expects a nil?"
  [vendor-id date available booked updated-at]
  (println "UPDATED-AT is: " updated-at)
  (if (= updated-at nil)
    (insert-calendar-day vendor-id date available booked)
    (update-calendar-day vendor-id date available booked updated-at)))

