(ns globar.db
  (:require [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]
            [clojure.string :as str])
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

;; this is the database component of the system
(defrecord GlobarDB [ds]

  component/Lifecycle

  (start [this]
    (assoc this
      :ds (pooled-data-source "localhost" "globardb" "globar_role" "j3mc" 25432)))

  (stop [this]
    (-> ds :datasource .close)
    (assoc this :ds nil)))

;; factory function to create a new database component
(defn new-db
  []
  {:db (map->GlobarDB {})})

(defn ^:private query
  "issue a query to the postgres db"
  [component statement]
  (let [[sql & params] statement]
    (log/debug :sql (str/replace sql #"\s+" " ")
               :params params))
  (jdbc/query (:ds component) statement))


(defn ^:private execute
  "execute a more complex SQL statement with postgres"
  [component statement]
  (let [[sql & params] statement]
    (log/debug :sql (str/replace sql #"\s+" " ")
               :params params))
  (jdbc/execute! (:ds component) statement {:multi? false}))

(defn find-customer-by-id
  "this is for internal use because internally we use the id for cross table joins"
  [component cust-id]
  (first
    (query component ["SELECT * FROM CUSTOMERS WHERE cust_id = ?" cust-id])))

(defn find-customer-by-email
  "this is the main client facing entrypoint as the email is the primary identifier for humans"
  [component cust-email]
  (let [result (query component ["SELECT * FROM CUSTOMERS WHERE email = ?" cust-email])]
    (first result)))

(defn find-vendor-by-id
  [component vendor-id]
  (first
    (query component ["SELECT * FROM VENDORS WHERE vendor_id = ?" vendor-id])))

(defn find-vendor-by-email
  "this is the main client facing entrypoint as the email is the primary identifier for humans"
  [component vendor-email]
  (let [result (query component ["SELECT * FROM VENDORS WHERE email = ?" vendor-email])]
    (first result)))

;; let's wait until it's clearer what this means in the business domain
;; could be a list of favorites, could be vendors the cust has transacted with
;; or something else
(defn list-vendors-for-customer
  [db cust-id]
  (let [vendors (:vendors (find-customer-by-id db cust-id))]
    (->> db
         :data
         deref
         :vendors
         (filter #(contains? vendors (:id %))))))
;; similarly defer the actual implementation of this
(defn list-customers-for-vendor
  [db vendor-id]
  (->> db
       :data
       deref
       :customers
       (filter #(-> % :vendors (contains? vendor-id)))))

(defn list-ratings-for-vendor
  "returns a list of ratings that have been submitted for a particular vendor"
  [component vendor-id]
  (query component
         ["SELECT vendor_id, cust_id, rating, created_at, updated_at
           FROM vendor_rating
           WHERE vendor_id = ?" vendor-id]))

(defn list-ratings-for-customer
  "returns a list of vendor ratings posted by the specified customer"
  [component cust-id]
  (query component
         ["SELECT vendor_id, cust_id, rating, created_at, updated_at
           FROM vendor_rating
           WHERE cust_id = ?" cust-id]))

(defn upsert-vendor-rating
  "Adds a new vendor rating, or changes the value to an existing rating if one exists"
  [component vendor-id cust-id rating]
  (log/debug :fn upsert-vendor-rating :vendor vendor-id :cust-id cust-id :rating rating)
  (execute component
         ["INSERT INTO vendor_rating (vendor_id, cust_id, rating)
           VALUES (?, ?, ?)
           ON CONFLICT (vendor_id, cust_id) DO UPDATE SET rating = EXCLUDED.rating"
          vendor-id cust-id rating])
  (log/debug :message "upsert-vendor-rating completed")
  nil)

(defn create-customer
  "Adds a new customer object, providing only the email address as the seed info"
  [component email]
  (log/debug :fn "create-customer" :email email)
  (execute component ["INSERT INTO CUSTOMERS (email) VALUES (?)" email])
  nil)

(defn update-customer
  "Adds a new customer object, or changes the values of an existing rating if one exists"
  [component new-cust]
  (let [{cust-id :cust_id
         name-first :name_first
         name-last :name_last
         password :password
         email :email
         addr-str-num :addr_str_num
         addr-str-name :addr_str_name
         addr-city :addr_city
         addr-state :addr_state
         addr-postal :addr_postal
         phone :phone
         locale :locale} new-cust]
    (jdbc/update! (:ds component) :Customers {:name_first name-first
                                              :name_last name-last
                                              :password password
                                              :email email
                                              :addr_str_num addr-str-num
                                              :addr_str_name addr-str-name
                                              :addr_city addr-city
                                              :addr_state addr-state
                                              :addr_postal addr-postal
                                              :phone phone
                                              :locale locale}
                  ["cust_id = ?" cust-id]))
  nil)

(defn create-vendor
  "Adds a new vendor object, providing only the email address as the seed info"
  [component email]
  (log/debug :fn "create-vendor" :email email)
  (execute component ["INSERT INTO VENDORS (email) VALUES (?)" email])
  nil)

(defn update-vendor
  "Takes a new-vendor map, destructures it to get the vendor attributes required to
  populate a string to send to the jdbc client and update the db"
  [component new-vendor]
  (let [{vendor-id :vendor_id
         name-first :name_first
         name-last :name_last
         password :password
         email :email
         addr-str-num :addr_str_num
         addr-str-name :addr_str_name
         addr-city :addr_city
         addr-state :addr_state
         addr-postal :addr_postal
         phone :phone
         locale :locale
         summary :summary} new-vendor]
    (jdbc/update! (:ds component) :Vendors {:name_first name-first
                                            :name_last name-last
                                            :password password
                                            :email email
                                            :addr_str_num addr-str-num
                                            :addr_str_name addr-str-name
                                            :addr_city addr-city
                                            :addr_state addr-state
                                            :addr_postal addr-postal
                                            :phone phone
                                            :locale locale
                                            :summary summary}
                  ["vendor_id = ?" vendor-id]))
  nil)

