(ns globar.db
  (:require [mount.core :refer [defstate]]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]
            [clojure.string :as str]
            [globar.config :as config]
            [fipp.edn :refer [pprint]])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))


(defn ^:private pooled-data-source
  "setup a worker pool of database connections"
  [host dbname user password port]
  {:datasource
   (doto (ComboPooledDataSource.)
     (.setDriverClass "org.postgresql.Driver")
     (.setJdbcUrl (str "jdbc:postgresql://" host ":" port "/" dbname))
     (.setUser user)
     (.setPassword password))})

(defn open-db-connection [conx-string]
  (println "opening db connection")
  (try
    (apply pooled-data-source conx-string)
    (catch  Exception e
      (str "caught exception: " (.getMessage e)))))

(defn close-db-connection [connection]
  (if connection
    (do
      (println "closing db connection")
      (-> connection :datasource .close))
    (println "no active connection to close")))

(defstate db-conn :start (open-db-connection (config/connection-string))
                  :stop (close-db-connection db-conn))

(defn query
  "issue a query to the postgres db"
  [statement]
  (let [[sql & params] statement]
    (log/debug :sql (str/replace sql #"\s+" " ")
               :params params))
  (jdbc/query db-conn statement {:identifiers #(.replace % \_\-)}))

(defn execute
  "execute a more complex SQL statement with postgres"
  [statement]
  (let [[sql & params] statement]
    (log/debug :sql (str/replace sql #"\s+" " ")
               :params params))
  (jdbc/execute! db-conn statement {:multi? false}))

(defn list-photos-by-ven-id
  "gets all photos for a vendor. owner param dictates whether or not you are allowed to see unpublished photos."
  [vendor-id owner?]
  (->> (query [(str "SELECT filename, service_id, description, published, 
                     TO_CHAR(created_at, 'YYYY-MM-DD')
                     FROM Images 
                     WHERE vendor_id = ? AND deleted = false "
                    (if (not owner?)
                      "AND published = true ")
                    "ORDER BY published asc, created_at desc") vendor-id])
      (map #(clojure.set/rename-keys % {:to-char :upload-date}))))

(defn find-user-by-id
  "this is for internal use because internally we use the id for cross table joins"
  [user-id]
  (-> (query ["SELECT * FROM Users WHERE user_id = ?" user-id])
      first))

(defn find-user-by-email
  "this is the main client facing entrypoint as the email is the primary identifier for humans"
  [user-email]
  (-> (query ["SELECT * FROM Users WHERE email = ?" user-email])
      first))

(defn find-user-by-sub
  [user-sub]
  (-> (query ["SELECT * FROM Users WHERE sub = ?" user-sub])
      first))

(defn find-vendor-by-id
  [vendor-id]
  (-> (query ["SELECT * FROM Vendors WHERE vendor_id = ?" vendor-id])
      first))

(defn find-vendor-by-user
  "lookup the vendor record by the associated user id"
  [user-id]
  (-> (query ["SELECT * FROM Vendors WHERE user_id = ?" user-id])
      first))

(defn find-vendor-by-email
  "this is the main client facing entrypoint as the email is the primary identifier for humans"
  [vendor-email]
  (-> (query ["SELECT * FROM Vendors WHERE email = ?" vendor-email])
      first))

(defn vendor-id-list
  "returns a list of vendor ids filtered by their city and their services"
  [city service]
  (if service
    ; join three tables (services, vendors, users) filter out rows that match
    ; city and service, and then return the unique vendor ids in the result
    (query ["SELECT DISTINCT vendor_id FROM (SELECT s.vendor_id FROM Services s
             INNER JOIN Vendors v
             ON s.vendor_id=v.vendor_id
             INNER JOIN Users u
             ON v.user_id=u.user_id
             WHERE UPPER(u.addr_city) = UPPER(?) AND s.s_type=?) AS FOO"
            city service])
    ; join two tables (vendors, users) filter out rows that match the city,
    ; then return the unique vendor ids in the result
    (query ["SELECT DISTINCT vendor_id FROM (SELECT v.vendor_id FROM Vendors v
             INNER JOIN Users u
             ON v.user_id=u.user_id
             WHERE UPPER(u.addr_city) = UPPER(?)) AS FOO" city])))

(defn list-services-for-vendor
  "returns a list of services that a particular vendor offers"
  [vendor-id]
  (query ["SELECT vendor_id, s_name, s_description, s_type, s_price, s_duration, service_id
               FROM Services
               WHERE vendor_id = ?" vendor-id]))

(defn list-bookings-for-vendor
  "returns a list of bookings for a specific vendor"
  [vendor-id]
  (->> (query ["SELECT vendor_id, booking_id, user_id, start_time, end_time, date, service
                    FROM Bookings
                    WHERE vendor_id = ?
                    ORDER BY date, start_time" vendor-id])
       (map #(update % :date str))
       (map #(assoc % :time [(:start-time %) (:end-time %)]))
       (map #(dissoc % :start-time :end-time))))

(defn list-ratings-for-vendor
  "returns a list of ratings that have been submitted for a particular vendor"
  [vendor-id]
  (query ["SELECT vendor_id, user_id, rating, created_at, updated_at
               FROM vendor_rating
               WHERE vendor_id = ?" vendor-id]))

(defn list-ratings-for-user
  "returns a list of vendor ratings posted by the specified user"
  [user-id]
  (query ["SELECT vendor_id, user_id, rating, created_at, updated_at
               FROM vendor_rating
               WHERE user_id = ?" user-id]))

(defn upsert-vendor-rating
  "Adds a new vendor rating, or changes the value to an existing rating if one exists"
  [vendor-id user-id rating]
  (log/debug :fn upsert-vendor-rating :vendor vendor-id :user-id user-id :rating rating)
  (execute ["INSERT INTO vendor_rating (vendor_id, user_id, rating)
             VALUES (?, ?, ?)
             ON CONFLICT (vendor_id, user_id)
             DO UPDATE SET rating = EXCLUDED.rating" vendor-id user-id rating])
  (log/debug :message "upsert-vendor-rating completed")
  nil)

(defn find-booking-by-id
  [booking-id]
  (-> (query ["SELECT * FROM Bookings WHERE booking_id = ?" booking-id])
      first
      (update :date str)
      (#(assoc % :time [(:start-time %) (:end-time %)]))
      (dissoc :start-time :end-time)))
                
