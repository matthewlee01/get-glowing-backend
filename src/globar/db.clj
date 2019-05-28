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
  (jdbc/query db-conn statement))


(defn execute
  "execute a more complex SQL statement with postgres"
  [statement]
  (let [[sql & params] statement]
    (log/debug :sql (str/replace sql #"\s+" " ")
               :params params))
  (jdbc/execute! db-conn statement {:multi? false}))

(defn find-customer-by-id
  "this is for internal use because internally we use the id for cross table joins"
  [cust-id]
  (first
    (query ["SELECT * FROM Customers WHERE cust_id = ?" cust-id])))

(defn find-customer-by-email
  "this is the main client facing entrypoint as the email is the primary identifier for humans"
  [cust-email]
  (let [result (query ["SELECT * FROM Customers WHERE email = ?" cust-email])]
    (first result)))

(defn find-customer-by-sub
  [cust-sub]
  (let [result (query ["SELECT * FROM Customers WHERE sub = ?" cust-sub])]
    (first result)))

(defn find-vendor-by-id
  [vendor-id]
  (first
    (query ["SELECT * FROM Vendors WHERE vendor_id = ?" vendor-id])))

(defn find-vendor-by-email
  "this is the main client facing entrypoint as the email is the primary identifier for humans"
  [vendor-email]
  (let [result (query ["SELECT * FROM Vendors WHERE email = ?" vendor-email])]
    (first result)))

(defn vendor-list
  "returns a list of vendors filtered by their city and their services"
  [city service]
  (if service
    (query ["SELECT DISTINCT * FROM (SELECT Vendors.* FROM Services
             INNER JOIN Vendors 
             ON Services.vendor_id=Vendors.vendor_id 
             WHERE UPPER(Vendors.addr_city) = UPPER(?) AND Services.s_type=?) AS FOO" 
            city service])
    (query ["SELECT * FROM Vendors WHERE UPPER(addr_city) = UPPER(?)" city])))

(defn list-services-for-vendor
  "returns a list of services that a particular vendor offers"
  [vendor-id]
  (query ["SELECT vendor_id, s_name, s_description, s_type, s_price, s_duration
           FROM Services
           WHERE vendor_id = ?" vendor-id]))

(defn list-ratings-for-vendor
  "returns a list of ratings that have been submitted for a particular vendor"
  [vendor-id]
  (query ["SELECT vendor_id, cust_id, rating, created_at, updated_at
           FROM vendor_rating
           WHERE vendor_id = ?" vendor-id]))

(defn list-ratings-for-customer
  "returns a list of vendor ratings posted by the specified customer"
  [cust-id]
  (query ["SELECT vendor_id, cust_id, rating, created_at, updated_at
           FROM vendor_rating
           WHERE cust_id = ?" cust-id]))

(defn upsert-vendor-rating
  "Adds a new vendor rating, or changes the value to an existing rating if one exists"
  [vendor-id cust-id rating]
  (log/debug :fn upsert-vendor-rating :vendor vendor-id :cust-id cust-id :rating rating)
  (execute ["INSERT INTO vendor_rating (vendor_id, cust_id, rating)
             VALUES (?, ?, ?)
             ON CONFLICT (vendor_id, cust_id)
             DO UPDATE SET rating = EXCLUDED.rating" vendor-id cust-id rating])
  (log/debug :message "upsert-vendor-rating completed")
  nil)

(defn create-customer
  "Adds a new customer object, or changes the values of an existing rating if one exists"
  [new-cust]
  (let [{ name-first :name_first
          name-last :name_last
          name :name
          password :password
          email :email
          email-verified :email_verified
          addr-str-num :addr_str_num
          addr-str-name :addr_str_name
          addr-city :addr_city
          addr-state :addr_state
          addr-postal :addr_postal
          phone :phone
          sub :sub
          avatar :avatar
          locale :locale} new-cust
        result (jdbc/insert! db-conn :Customers{:name_first name-first
                                                :name_last name-last
                                                :name name
                                                :password password
                                                :email email
                                                :email_verified email-verified
                                                :addr_str_num addr-str-num
                                                :addr_str_name addr-str-name
                                                :addr_city addr-city
                                                :addr_state addr-state
                                                :addr_postal addr-postal
                                                :phone phone
                                                :sub sub
                                                :avatar avatar
                                                :locale locale})]
    (first result)))
                  

(defn update-customer
  "Adds a new customer object, or changes the values of an existing rating if one exists"
  [new-cust]
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
         locale :locale} new-cust
        result (jdbc/update! db-conn 
                             :Customers 
                             {:name_first name-first
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
                             ["cust_id = ?" cust-id])]
    (println "TODO: add error checking to this!! ")
    (pprint result)))

(defn create-vendor
  [new-vendor]
  (let [{name-first :name_first
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
         summary :summary} new-vendor
         result (jdbc/insert! db-conn :Vendors {:name_first name-first
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
                                                :summary summary})]
    (first result)))
  


(defn update-vendor
  [vendor]
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
         summary :summary} vendor
         result (jdbc/update! db-conn 
                              :Vendors 
                              {:name_first name-first
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
                              ["vendor_id = ?" vendor-id])]
    (println "TODO: add error checking to this!! ")
    (pprint result)))
