(ns globar.images.db
  (:require [io.pedestal.log :as log]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [globar.config :as config]
            [io.pedestal.log :as log]
            [globar.db :as db]
            [clojure.java.jdbc :as jdbc]))

(defn find-image-by-filename
  [filename]
  (-> (db/query ["SELECT * FROM Images
                  WHERE filename = ?" filename])
      first))

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

(defn update-image
  "updates an image in the table by filename"
  [image]
  (if-let [filename (:filename image)]
    (let [existing-image (find-image-by-filename filename)
          new-image (merge existing-image image)
          {:keys [filename vendor-id metadata description published deleted service-id]} new-image
          result (jdbc/update! db/db-conn
                              :Images {:filename filename
                                       :vendor_id vendor-id
                                       :metadata metadata
                                       :description description
                                       :published published
                                       :deleted deleted
                                       :service_id service-id}
                              ["filename = ?" filename])]
      (first result))
    (log/debug :message "file not found")))

(defn ven-publish-all
  [ven-id]
  (db/execute ["UPDATE Images
               SET published = true
               WHERE vendor_id = ?" ven-id]))

