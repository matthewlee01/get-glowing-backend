(ns globar.db
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]))

(defrecord GlobarDB [data]

  component/Lifecycle

  (start [this]
    (assoc this :data (-> (io/resource "globar-data.edn")
                          slurp
                          edn/read-string
                          atom)))

  (stop [this]
    (assoc this :data nil)))

(defn new-db
  []
  {:db (map->GlobarDB {})})


(defn find-user-by-id
  [db user-id]
  (->> db
       :data
       deref
       :users
       (filter #(= user-id (:id %)))
       first))

(defn find-vendor-by-id
  [db vendor-id]
  (->> db
       :data
       deref
       :vendors
       (filter #(= vendor-id (:id %)))
       first))

(defn list-vendors-for-user
  [db user-id]
  (let [vendors (:vendors (find-user-by-id db user-id))]
    (->> db
         :data
         deref
         :vendors
         (filter #(contains? vendors (:id %))))))

(defn list-users-for-vendor
  [db vendor-id]
  (->> db
       :data
       deref
       :users
       (filter #(-> % :vendors (contains? vendor-id)))))

(defn list-ratings-for-vendor
  [db vendor-id]
  (->> db
       :data
       deref
       :ratings
       (filter #(= vendor-id (:vendor_id %)))))

(defn list-ratings-for-user
  [db user-id]
  (->> db
       :data
       deref
       :ratings
       (filter #(= user-id (:user_id %)))))

(defn ^:private apply-vendor-rating
  [vendor-ratings vendor-id user-id rating]
  (->> vendor-ratings
       (remove #(and (= vendor-id (:vendor_id %))
                     (= user-id (:user_id %))))
       (cons {:vendor_id vendor-id
              :user_id user-id
              :rating rating})))

(defn upsert-vendor-rating
  "Adds a new vendor rating, or changes the value of an existing rating."
  [db vendor-id user-id rating]
  (-> db
      :data
      (swap! update :ratings apply-vendor-rating vendor-id user-id rating)))