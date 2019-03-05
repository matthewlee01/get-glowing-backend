(ns globar.schema
  "Contains custom resolvers and defines the schema provider component"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :refer [resolve-as]]
            [com.stuartsierra.component :as component]
            [globar.db :as db]
            [clojure.edn :as edn]
            [io.pedestal.log :as log]))

(defn customer-by-email
  [db]
  (fn [_ args _]
    (db/find-customer-by-email db (:email args))))

(defn vendor-by-email
  [db]
  (fn [_ args _]
    (db/find-vendor-by-email db (:email args))))

(defn rate-vendor
  [db]
  (fn [_ args _]
    (let [{vendor-id :vendor_id
           cust-id :cust_id
           rating :rating} args
          vendor (db/find-vendor-by-id db vendor-id)
          customer (db/find-customer-by-id db cust-id)]
      (cond
        (nil? vendor)
        (resolve-as nil {:message "Vendor not found."
                         :status 404})

        (nil? customer)
        (resolve-as nil {:message "Customer not found."
                         :status 404})

        (not (<= 1 rating 5))
        (resolve-as nil {:message "Rating must be between 1 and 5."
                         :status 400})

        :else
        (do
          (db/upsert-vendor-rating db vendor-id cust-id rating)
          vendor)))))

(defn create-customer
  "this handles the case where we are creating a customer for the first time"
  [db]
  (fn [_ args _]
    (log/debug :fn "create-customer" :args args)
    (let [email (:email (:new_cust args))]
      (db/create-customer db email)
      (db/find-customer-by-email db email))))

(defn update-customer
  "this resolver handles the update-customer mutation"
  [db]
  (fn [_ args _]
    (let [cust-id (:cust_id (:upd_cust args))
          old-cust (db/find-customer-by-id db cust-id)
          new-cust (merge old-cust (:upd_cust args))]
      (do
        (log/debug :fn "update-customer" :old-cust old-cust :new-cust new-cust)
        (db/update-customer db new-cust)
        (db/find-customer-by-id db cust-id)))))

(defn create-vendor
  "this handles the case where we are creating a vendor for the first time"
  [db]
  (fn [_ args _]
    (log/debug :fn "create-vendor" :args args)
    (let [email (:email (:new_vendor args))]
      (db/create-vendor db email)
      (db/find-vendor-by-email db email))))

(defn update-vendor
  "this resolver handles the update-vendor mutation"
  [db]
  (fn [_ args _]
    (let [vendor-id (:vendor_id (:upd_vendor args))
          old-vendor (db/find-customer-by-id db vendor-id)
          new-vendor (merge old-vendor (:upd_vendor args))]
      (do
        (log/debug :fn "update-vendor" :old-vendor old-vendor :new-vendor new-vendor)
        (db/update-vendor db new-vendor)
        (db/find-vendor-by-id db vendor-id)))))

(defn customer-vendorlist
  [db]
  (fn [_ _ user]
    (db/list-vendors-for-customer db (:cust_id user))))

(defn vendor-userlist
  [db]
  (fn [_ _ vendor]
    (db/list-customers-for-vendor db (:vendor_id vendor))))

(defn rating-summary
  [db]
  (fn [_ _ vendor]
    (let [ratings (map :rating (db/list-ratings-for-vendor db (:vendor_id vendor)))
          n (count ratings)]
      (log/debug :ratings ratings)
      {:count n
       :average (if (zero? n)
                  0
                  (/ (apply + ratings)
                     (float n)))})))
(defn customer-ratings
  [db]
  (fn [_ _ customer]
    (db/list-ratings-for-customer db (:user_id customer))))

(defn vendor-ratings
  "this pulls a list of all the ratings that have been submitted for a vendor"
  [db]
  (fn [_ _ vendor]
    (db/list-ratings-for-vendor db (:vendor_id vendor))))

(defn vendor-rating->vendor
  "returns a function that returns a vendor object related to a given
  vendor rating record"
  [db]
  (fn [_ _ rating]
    (db/find-vendor-by-id db (:vendor_id rating))))

(defn resolver-map
  "update resolver symbols in the schema with actual resolver function references"
  [component]
  (let [db (:db component)]
    {:query/customer-by-email    (customer-by-email db)
     :query/vendor-by-email  (vendor-by-email db)
     :mutation/rate-vendor (rate-vendor db)
     :mutation/create-customer (create-customer db)
     :mutation/update-customer (update-customer db)
     :mutation/create-vendor (create-vendor db)
     :mutation/update-vendor (update-vendor db)
     :Customer/vendors        (customer-vendorlist db)
     :Customer/vendor-ratings (customer-ratings db)
     :Vendor/customers        (vendor-userlist db)
     :Vendor/rating-summary (rating-summary db)
     :Vendor/vendor-ratings (vendor-ratings db)
     :VendorRating/vendor (vendor-rating->vendor db)}))

(defn load-schema
  "this function reads the schema file from disk, replaces resolver symbols
  with function references, and compiles into a lacinia schema"
  [component]
  (-> (io/resource "globar-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map component))
      schema/compile))


;; this is a record that implements the Lifecycle protocol
;; and calls the load-schema function on start to get a compiled
;; lacinia schema, which is then stored in the :schema key of
;; this record
(defrecord SchemaProvider [schema]
  component/Lifecycle

  (start [this]
    (assoc this :schema (load-schema this)))

  (stop [this]
    (assoc this :schema nil)))

;; a constructor for the SchemaProvider record
(defn new-schema-provider
  []
  {:schema-provider (-> {}
                        map->SchemaProvider
                        (component/using [:db]))})
