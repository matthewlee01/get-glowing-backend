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

(defn user-by-email
  [_ args _]
  (db/find-user-by-email (:email args)))

(defn vendor-by-email
  [_ args _]
  (let [user (db/find-user-by-email (:email args))
        vendor (db/find-vendor-by-user (:user-id user))]
    (merge user vendor)))

(defn- vendor-by-id-internal
  [id]
  (let [vendor (db/find-vendor-by-id id)
        user (db/find-user-by-id (:user-id vendor))]
    (merge user vendor)))

(defn vendor-by-id
  [_ args _]
  (vendor-by-id-internal (:id args)))

(defn vendor-list
  [_ args _]
  ; get a list of vendor ids that match the criteria
  (let [id-list (db/vendor-id-list (:addr_city args) (:service args))]
    ; get a vendor object filled out for each id using the vendor-by-id fn
    (map #(vendor-by-id-internal (:vendor-id %)) id-list)))

(defn rate-vendor
  [_ args _]
  (let [{vendor-id :vendor-id
         user-id :user-id
         rating :rating} args
         vendor (db/find-vendor-by-id vendor-id)
         user (db/find-user-by-id user-id)]
    (cond
      (nil? vendor)
      (resolve-as nil {:message "Vendor not found."
                       :status 404})

      (nil? user)
      (resolve-as nil {:message "User not found."
                       :status 404})

      (not (<= 1 rating 5))
      (resolve-as nil {:message "Rating must be between 1 and 5."
                       :status 400})

      :else
      (do
        (db/upsert-vendor-rating vendor-id user-id rating)
        vendor))))

(defn create-user
  "this handles the case where we are creating a user for the first time"
  [_ args _]
  (log/debug :fn "create-user" :args args)
  (let [email (:email (:new_user args))]
    (db/create-user email)
    (db/find-user-by-email email)))

(defn update-user
  "this resolver handles the update-user mutation"
  [_ args _]
  (let [user-id (:user-id (:upd_user args))
        old-user (db/find-user-by-id user-id)
        new-user (merge old-user (:upd_user args))]
    (do
      (log/debug :fn "update-user" :old-user old-user :new-user new-user)
      (db/update-user new-user)
      (db/find-user-by-id user-id))))

(defn create-vendor
  "this handles the case where we are creating a vendor for the first time"
  [_ args _]
  (log/debug :fn "create-vendor" :args args)
  (let [email (:email (:new_vendor args))
        passwd (:password (:new_vendor args))]
    (db/create-vendor email passwd)
    (db/find-vendor-by-email email)))

(defn update-vendor
  "this resolver handles the update-vendor mutation"
  [_ args _]
  (let [vendor-id (:vendor-id (:upd_vendor args))
        old-vendor (db/find-user-by-id vendor-id)
        new-vendor (merge old-vendor (:upd_vendor args))]
    (do
      (log/debug :fn "update-vendor" :old-vendor old-vendor :new-vendor new-vendor)
;;      (db/update-vendor new-vendor)
      (db/find-vendor-by-id vendor-id))))

(defn user-vendorlist
  [_ _ user]
  nil)

(defn vendor-userlist
  [_ _ vendor]
  nil)

(defn vendor-services
  [_ _ vendor]
  (db/list-services-for-vendor (:vendor-id vendor)))

(defn services-summary
  [_ _ vendor]
  (let [prices (map :s-price (db/list-services-for-vendor (:vendor-id vendor)))
        svc-count (count prices)
        svc-min (apply min prices)
        svc-max (apply max prices)]
    {:count svc-count
     :min svc-min 
     :max svc-max}))
                      
                      
(defn rating-summary
  [_ _ vendor]
  (let [ratings (map :rating (db/list-ratings-for-vendor (:vendor-id vendor)))
        n (count ratings)]
    (log/debug :ratings ratings)
    {:count n
     :average (if (zero? n)
                0
                (/ (apply + ratings)
                   (float n)))}))

(defn user-ratings [_ _ user]
    (db/list-ratings-for-user (:user-id user)))

(defn vendor-ratings
  "this pulls a list of all the ratings that have been submitted for a vendor"
  [_ _ vendor]
  (db/list-ratings-for-vendor (:vendor-id vendor)))

(defn vendor-rating->vendor
  "returns a function that returns a vendor object related to a given
  vendor rating record"
  [_ _ rating]
  (db/find-vendor-by-id (:vendor-id rating)))

(defn resolver-map []
  "update resolver symbols in the schema with actual resolver function references"
  {:query/user-by-email        user-by-email
   :query/vendor-by-email      vendor-by-email
   :query/vendor-list          vendor-list
   :query/vendor-by-id         vendor-by-id
   :mutation/rate-vendor       rate-vendor
   :mutation/create-user       create-user
   :mutation/update-user       update-user
   :mutation/create-vendor     create-vendor
   :mutation/update-vendor     update-vendor
   :User/vendors               user-vendorlist
   :User/vendor-ratings        user-ratings
   :Vendor/users               vendor-userlist
   :Vendor/services            vendor-services
   :Vendor/services-summary    services-summary
   :Vendor/rating-summary      rating-summary
   :Vendor/vendor-ratings      vendor-ratings
   :VendorRating/vendor        vendor-rating->vendor})

(defn load-schema []
  "this function reads the schema file from disk, replaces resolver symbols
  with function references, and compiles into a lacinia schema"
  (println "loading schema")
  (let [s (-> (io/resource "globar-schema.edn")
              slurp
              edn/read-string
              (util/attach-resolvers (resolver-map)))]
    (schema/compile s {:default-field-resolver schema/hyphenating-default-field-resolver})))

(defn load-schema2 []
  "this function reads the schema file from disk, replaces resolver symbols
  with function references, and compiles into a lacinia schema"
  (println "loading schema")
  (-> (io/resource "globar-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      (schema/compile {:default-field-resolver schema/hyphenating-default-field-resolver})))


(defstate schema-state :start (load-schema2))
