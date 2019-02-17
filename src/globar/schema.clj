(ns globar.schema
  "Contains custom resolvers and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.resolve :refer [resolve-as]]
            [com.stuartsierra.component :as component]
            [globar.db :as db]
            [clojure.edn :as edn]
            [io.pedestal.log :as log]))

(defn user-by-id
  [db]
  (fn [_ args _]
    (db/find-user-by-id db (:id args))))

(defn vendor-by-id
  [db]
  (fn [_ args _]
    (db/find-vendor-by-id db (:id args))))

(defn rate-vendor
  [db]
  (fn [_ args _]
    (let [{vendor-id :vendor_id
           user-id :user_id
           rating :rating} args
          vendor (db/find-vendor-by-id db vendor-id)
          user (db/find-user-by-id db user-id)]
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
          (db/upsert-vendor-rating db vendor-id user-id rating)
          vendor)))))



(defn user-vendorlist
  [db]
  (fn [_ _ user]
    (db/list-vendors-for-user db (:user_id user))))

(defn vendor-userlist
  [db]
  (fn [_ _ vendor]
    (db/list-users-for-vendor db (:vendor_id vendor))))

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
(defn user-ratings
  [db]
  (fn [_ _ user]
    (db/list-ratings-for-user db (:user_id user))))

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
    {:query/user-by-id    (user-by-id db)
     :query/vendor-by-id  (vendor-by-id db)
     :mutation/rate-vendor (rate-vendor db)
     :User/vendors        (user-vendorlist db)
     :User/vendor-ratings (user-ratings db)
     :Vendor/users        (vendor-userlist db)
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
