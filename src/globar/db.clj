(ns globar.db
  (:require [com.stuartsierra.component :as component]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]
            [clojure.string :as str])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource)))


(defn ^:private pooled-data-source
  [host dbname user password port]
  {:datasource
   (doto (ComboPooledDataSource.)
     (.setDriverClass "org.postgresql.Driver")
     (.setJdbcUrl (str "jdbc:postgresql://" host ":" port "/" dbname))
     (.setUser user)
     (.setPassword password))})

(defrecord GlobarDB [ds]

  component/Lifecycle

  (start [this]
    (assoc this
      :ds (pooled-data-source "localhost" "globardb" "globar_role" "j3mc" 25432)))

  (stop [this]
    (-> ds :datasource .close)
    (assoc this :ds nil)))

(defn new-db
  []
  {:db (map->GlobarDB {})})

(defn ^:private query
  [component statement]
  (let [[sql & params] statement]
    (log/debug :sql (str/replace sql #"\s+" " ")
               :params params))
  (jdbc/query (:ds component) statement))

(defn ^:private execute
  [component statement]
  (let [[sql & params] statement]
    (log/debug :sql (str/replace sql #"\s+" " ")
               :params params))
  (jdbc/execute! (:ds component) statement {:multi? false}))

(defn find-user-by-id
  [component user-id]
  (first
    (query component
           ["SELECT user_id, name, password, created_at, updated_at
             FROM USERS
             WHERE user_id = ?" user-id])))

(defn find-vendor-by-id
  [component vendor-id]
  (first
    (query component
           ["select vendor_id, name, summary, created_at, updated_at
            from vendor where vendor_id = ?" vendor-id])))

;; let's wait until it's clearer what this means in the business domain
;; could be a list of favorites, could be vendors the user has transacted with
;; or something else
(defn list-vendors-for-user
  [db user-id]
  (let [vendors (:vendors (find-user-by-id db user-id))]
    (->> db
         :data
         deref
         :vendors
         (filter #(contains? vendors (:id %))))))
;; similarly defer the actual implementation of this
(defn list-users-for-vendor
  [db vendor-id]
  (->> db
       :data
       deref
       :users
       (filter #(-> % :vendors (contains? vendor-id)))))

(defn list-ratings-for-vendor
  "returns a list of ratings that have been submitted for a particular vendor"
  [component vendor-id]
  (query component
         ["SELECT vendor_id, user_id, rating, created_at, updated_at
           FROM vendor_rating
           WHERE vendor_id = ?" vendor-id]))

(defn list-ratings-for-user
  "returns a list of vendor ratings posted by the specified user"
  [component user-id]
  (query component
         ["SELECT vendor_id, user_id, rating, created_at, updated_at
           FROM vendor_rating
           WHERE user_id = ?" user-id]))

(defn upsert-vendor-rating
  "Adds a new vendor rating, or changes the value to an existing rating if one exists"
  [component vendor-id user-id rating]
  (log/debug :vendor vendor-id :user user-id :rating rating)
  (execute component
         ["INSERT INTO vendor_rating (vendor_id, user_id, rating)
           VALUES (?, ?, ?)
           ON CONFLICT (vendor_id, user_id) DO UPDATE SET rating = EXCLUDED.rating"
          vendor-id user-id rating])
  (log/debug :message "all clear after upsert")
  nil)


