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
  (let [field-spec (select-keys image [:deleted :vendor-id :filename :published :metadata :service-id :description]) 
        db-field-spec (clojure.set/rename-keys field-spec
                                               {:vendor-id :vendor_id
                                                :service-id :service_id})
        result (jdbc/insert! db/db-conn
                             :Images 
                             db-field-spec
                             {:identifiers #(.replace % \_\-)})]
    (first result)))

(defn ven-publish-photo
  [filename]
  (db/execute ["UPDATE Images
               SET published = true
               WHERE filename = ?" filename]))

(defn ven-publish-all
  [ven-id]
  (db/execute ["UPDATE Images
               SET published = true
               WHERE vendor_id = ?" ven-id]))

(defn ven-delete-photo
  [filename]
  (db/execute ["UPDATE Images
                SET deleted = true
                WHERE filename = ?" filename]))
  ;;perhaps add some code here in the future to delete the actual file
