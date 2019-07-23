(ns globar.images.db
  (:require [io.pedestal.log :as log]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [globar.config :as config]
            [globar.db :as db]
            [clojure.java.jdbc :as jdbc]))


(defn create-image
  "Adds a new image record to the Images table"
  [image]
  (log/debug ::create-image image)
  (let [field-spec (select-keys image [:vendor-id :filename :metadata :service-id :description]) 
        db-field-spec (clojure.set/rename-keys field-spec
                                               {:vendor-id :vendor_id
                                                :service-id :service_id})
        result (jdbc/insert! db/db-conn
                             :Images 
                             db-field-spec
                             {:identifiers #(.replace % \_\-)})]
    (first result)))

(defn get-images
  "Return all the images for a given vendor"
  [vendor-id]
  (log/debug :function ::get-images :vendor-id vendor-id) 
  (db/query ["SELECT * FROM Images WHERE vendor_id = ?" vendor-id]))

