(ns globar.schema
  "Contains custom resolvers and a function to provide the full schema"
  (:require [clojure.java.io :as io]
            [com.walmartlabs.lacinia.util :as util]
            [com.walmartlabs.lacinia.schema :as schema]
            [clojure.edn :as edn]))

(defn resolve-user-by-id
  "resolves a user given a user id"
  [user-map context args value]
;;  (println "in resolve-user-by-id")
;;  (println user-map)
  (let [{:keys [id]} args]
    (get user-map id)))

(defn resolve-user-vendors
  "resolves a users vendors field by taking the field and filtering
  out all the vendors specified by id in the vendors field"
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

(defn entity-map [data k]
  (reduce #(assoc %1 (:id %2) %2)
          {}
          (get data k)))


(defn resolver-map []
  (let [data (-> (io/resource "globar-data.edn")
                 slurp
                 edn/read-string)
        users-map (entity-map data :users)
        vendors-map (entity-map data :vendors)]

    {:query/user-by-id (partial resolve-user-by-id users-map)
     :User/vendors (partial resolve-user-vendors vendors-map)
     :Vendor/users (partial resolve-vendor-users users-map)}))



(defn load-schema []
  (-> (io/resource "globar-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map))
      schema/compile))


