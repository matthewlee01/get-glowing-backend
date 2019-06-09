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
        svc-count (count prices)]
    (if (> svc-count 0)
      (let [svc-min (apply min prices)
            svc-max (apply max prices)]
         {:count svc-count
          :min svc-min
          :max svc-max})
      {})))
                      
                      
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

(defstate schema-state :start (load-schema))
