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
  (println "list-services-for-vendor: " vendor-id)
  (query ["SELECT vendor_id, s_name, s_description, s_type, s_price, s_duration, service_id
               FROM Services
               WHERE vendor_id = ?" vendor-id]))

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
      first))

(defn create-user
  "Adds a new user object, or changes the values of an existing rating if one exists"
  [user-info]
  (log/debug ::create-user user-info)
  (let [field-spec (select-keys user-info [:name-first :name-last :name :email :email-verified
                                           :is-vendor :addr-str-num :addr-str-name :addr-city
                                           :addr-state :addr-postal :phone :locale :avatar :sub])
        db-field-spec (clojure.set/rename-keys field-spec
                                               {:name-first :name_first
                                                :name-last :name_last
                                                :email-verified :email_verified
                                                :is-vendor :is_vendor
                                                :addr-str-num :addr_str_num
                                                :addr-str-name :addr_str_name
                                                :addr-city :addr_city
                                                :addr-state :addr_state
                                                :addr-postal :addr_postal})
        result (jdbc/insert! db-conn :Users 
                             db-field-spec
                             {:identifiers #(.replace % \_\-)})]
    (first result)))

(defn update-user
  "Adds a new user object, or changes the values of an existing rating if one exists"
  [new-user]
  (log/debug ::update-user new-user)
  (let [field-spec (select-keys new-user [:name-first :name-last :name :email :email-verified
                                          :is-vendor :addr-str-num :addr-str-name :addr-city
                                          :addr-state :addr-postal :phone :locale :avatar :sub])
        db-field-spec (clojure.set/rename-keys field-spec
                                            {:name-first :name_first
                                             :name-last :name_last
                                             :email-verified :email_verified
                                             :is-vendor :is_vendor
                                             :addr-str-num :addr_str_num
                                             :addr-str-name :addr_str_name
                                             :addr-city :addr_city
                                             :addr-state :addr_state
                                             :addr-postal :addr_postal})]

    (jdbc/update! db-conn :Users db-field-spec ["user_id = ?" (:user-id new-user)])
    (println "TODO: add error checking to this!! ")))

(defn create-vendor
  [new-vendor]
  (let [{:keys [user-id 
                summary 
                profile-pic]} new-vendor
        updated-user (-> (find-user-by-id user-id)
                         (merge new-vendor)
                         (assoc :is-vendor true) ;; set the vendor flag on the associated user object
                         (update-user))
        result (jdbc/insert! db-conn :Vendors {:user_id user-id
                                               :summary summary
                                               :profile_pic profile-pic}
                             {:identifiers #(.replace % \_\-)})]

    (first result)))

(defn update-vendor
  [vendor]
  (let [{vendor-id :vendor-id
         summary :summary} vendor
         result (jdbc/update! db-conn 
                              :Vendors 
                              {:summary summary}
                              ["vendor_id = ?" vendor-id])]
    (println "TODO: add error checking to this!! " result)
    (if (= 0 (first result))
      (println "ERROR UPDATING VENDOR"))))

(defn create-service
  [new-service]
  (log/debug :function-name ::create-service :new-service new-service)
  (let [field-spec (select-keys new-service [:vendor-id :s-name :s-description :s-type :s-price :s-duration])
        db-field-spec (clojure.set/rename-keys field-spec
                                               {:vendor-id :vendor_id
                                                :s-name :s_name
                                                :s-description :s_description
                                                :s-type :s_type
                                                :s-price :s_price
                                                :s-duration :s_duration})
        result (jdbc/insert! db-conn
                             :Services 
                             db-field-spec
                             {:identifiers #(.replace % \_\-)})]
    (first result)))

(defn update-service
  [service]
  (log/debug :function-name ::update-service :service service)
  (let [field-spec (select-keys service [:vendor-id :s-name :s-description :s-type :s-price :s-duration])
        db-field-spec (clojure.set/rename-keys field-spec
                                               {:vendor-id :vendor_id
                                                :s-name :s_name
                                                :s-description :s_description
                                                :s-type :s_type
                                                :s-price :s_price
                                                :s-duration :s_duration})
        result (jdbc/update! db-conn
                             :Services                              
                             db-field-spec
                             ["vendor_id = ? and service_id = ?" (:vendor-id service) (:service-id service)])]
    (println "TODO: add error checking to this!! " result)
    (if (= 0 (first result))
      (println "ERROR UPDATING SERVICE"))))

(defn create-booking
  [booking]
  (let [{:keys [vendor-id user-id time date service]} booking
        result (jdbc/insert! db-conn
                             :Bookings {:vendor_id vendor-id
                                        :user_id user-id
                                        :time (prn-str time)
                                        :date date
                                        :service service
                                        :cancelled false}
                             {:identifiers #(.replace % \_\-)})]
    (first result)))

(defn update-booking
 [new-booking-info]
 (let [{:keys [booking-id]} new-booking-info
       {:keys [vendor-id user-id time date service cancelled]} (-> (find-booking-by-id booking-id)
                                                                   (merge new-booking-info))
       result (jdbc/update! db-conn
                             :Bookings {:vendor_id vendor-id
                                        :user_id user-id
                                        :time time
                                        :date date
                                        :service service
                                        :cancelled cancelled}
                             ["booking_id = ?" booking-id])]
   result))
       
