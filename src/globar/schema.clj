(ns globar.schema
  "Contains custom resolvers and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.stuartsierra.component :as component]
            [clojure.edn :as edn]))

(defn resolve-element-by-id
  "extracts an id from the parameter, and uses it to return
   the value at this index from the element-map - assumes the element map
   has keys that represent ids"
  [element-map context args value]
  (let [{:keys [id]} args]
    (get element-map id)))

(defn resolve-user-vendors
  "resolves a User's vendors field by taking the field (a set of ids) and
  filtering out all the vendors based on these ids"
  [vendors-map context args user]
  (->> user
       :vendors
       (map vendors-map)))

(defn resolve-vendor-users
  "resolves a vendor object's users field by going through each user
  and filtering out those that don't reference this vendor's id"
  [users-map context args vendor]
  (let [{:keys [id]} vendor]
    (->> users-map
         vals
         (filter #(-> % :vendors (contains? id))))))


(defn entity-map
  "create a map that is keyed off ids, instead of a flat sequence"
  [data k]
  (reduce #(assoc %1 (:id %2) %2)
          {}
          (get data k)))

(defn rating-summary
 [data]
 (fn [_ _ vendor]
   (let [id (:id vendor)
         ratings (->> data
                      :ratings
                      (filter #(= id (:vendor_id %)))
                      (map :ratings))
         n (count ratings)]
     {:count n
      :average (if (zero? n)
                 0
                 (/ (apply + ratings)
                    (float n)))})))

(defn vendor-ratings
  "returns a function that calculates gets all the ratings a user has submitted"
  [ratings-map]
  (fn [_ _ user]
    (let [id (:id user)]
      (filter #(= id (:user_id %)) ratings-map))))


(defn vendor-rating->vendor
  "returns a function that returns a vendor object related to a given vendor-rating"
  [vendors-map]
  (fn [_ _ rating]
    (get vendors-map (:vendor_id rating))))


(defn resolver-map
  "update resolver symbols in the schema with actual resolver function references"
  [component]
  (let [data (-> (io/resource "globar-data.edn")
                 slurp
                 edn/read-string)
        users-map (entity-map data :users)
        vendors-map (entity-map data :vendors)]

    {:query/user-by-id    (partial resolve-element-by-id users-map)
     :query/vendor-by-id  (partial resolve-element-by-id vendors-map)
     :User/vendors        (partial resolve-user-vendors vendors-map)
     :User/vendor-ratings (vendor-ratings (:ratings data))
     :Vendor/users        (partial resolve-vendor-users users-map)
     :Vendor/rating-summary (rating-summary data)
     :VendorRating/vendor (vendor-rating->vendor vendors-map)}))


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
  {:schema-provider (map->SchemaProvider {})})
