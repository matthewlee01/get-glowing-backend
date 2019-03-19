(ns globar.schema
  "Contains custom resolvers and defines the resolved schema map"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :refer [resolve-as]]
            [mount.core :refer [defstate]]
            [globar.db :as db]
            [clojure.edn :as edn]
            [io.pedestal.log :as log]))

(defn customer-by-email
  [_ args _]
  (db/find-customer-by-email (:email args)))

(defn vendor-by-email
  [_ args _]
  (db/find-vendor-by-email (:email args)))

(defn vendor-list
  [_ args _]
  (db/vendor-list (:addr_city args) (:service args)))

(defn rate-vendor
  [_ args _]
  (let [{vendor-id :vendor_id
         cust-id :cust_id
         rating :rating} args
         vendor (db/find-vendor-by-id vendor-id)
         customer (db/find-customer-by-id cust-id)]
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
        (db/upsert-vendor-rating vendor-id cust-id rating)
        vendor))))

(defn create-customer
  "this handles the case where we are creating a customer for the first time"
  [_ args _]
  (log/debug :fn "create-customer" :args args)
  (let [email (:email (:new_cust args))]
    (db/create-customer email)
    (db/find-customer-by-email email)))

(defn update-customer
  "this resolver handles the update-customer mutation"
  [_ args _]
  (let [cust-id (:cust_id (:upd_cust args))
        old-cust (db/find-customer-by-id cust-id)
        new-cust (merge old-cust (:upd_cust args))]
    (do
      (log/debug :fn "update-customer" :old-cust old-cust :new-cust new-cust)
      (db/update-customer new-cust)
      (db/find-customer-by-id cust-id))))

(defn create-vendor
  "this handles the case where we are creating a vendor for the first time"
  [_ args _]
  (log/debug :fn "create-vendor" :args args)
  (let [email (:email (:new_vendor args))]
    (db/create-vendor email)
    (db/find-vendor-by-email email)))

(defn update-vendor
  "this resolver handles the update-vendor mutation"
  [_ args _]
  (let [vendor-id (:vendor_id (:upd_vendor args))
        old-vendor (db/find-customer-by-id vendor-id)
        new-vendor (merge old-vendor (:upd_vendor args))]
    (do
      (log/debug :fn "update-vendor" :old-vendor old-vendor :new-vendor new-vendor)
      (db/update-vendor new-vendor)
      (db/find-vendor-by-id vendor-id))))

(defn customer-vendorlist
  [_ _ user]
  nil)

(defn vendor-userlist
  [_ _ vendor]
  nil)

(defn rating-summary
  [_ _ vendor]
  (let [ratings (map :rating (db/list-ratings-for-vendor (:vendor_id vendor)))
        n (count ratings)]
    (log/debug :ratings ratings)
    {:count n
     :average (if (zero? n)
                0
                (/ (apply + ratings)
                   (float n)))}))

(defn customer-ratings [_ _ customer]
    (db/list-ratings-for-customer (:user_id customer)))

(defn vendor-ratings
  "this pulls a list of all the ratings that have been submitted for a vendor"
  [_ _ vendor]
  (db/list-ratings-for-vendor (:vendor_id vendor)))

(defn vendor-rating->vendor
  "returns a function that returns a vendor object related to a given
  vendor rating record"
  [_ _ rating]
  (db/find-vendor-by-id (:vendor_id rating)))

(defn resolver-map []
  "update resolver symbols in the schema with actual resolver function references"
  {:query/customer-by-email    customer-by-email
   :query/vendor-by-email      vendor-by-email
   :query/vendor-list          vendor-list
   :mutation/rate-vendor       rate-vendor
   :mutation/create-customer   create-customer
   :mutation/update-customer   update-customer
   :mutation/create-vendor     create-vendor
   :mutation/update-vendor     update-vendor
   :Customer/vendors           customer-vendorlist
   :Customer/vendor-ratings    customer-ratings
   :Vendor/customers           vendor-userlist
   :Vendor/rating-summary      rating-summary
   :Vendor/vendor-ratings      vendor-ratings
   :VendorRating/vendor        vendor-rating->vendor})

(defn load-schema []
  "this function reads the schema file from disk, replaces resolver symbols
  with function references, and compiles into a lacinia schema"
  (-> (io/resource "globar-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))

(defstate schema-state :start (load-schema))
