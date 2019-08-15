(ns globar.ven-details.db
  (:require [globar.db :as db]))

(defn get-ven-user
  [vendor-id]
  "finds a vendor and all of their relevant associated user data"
  (-> (db/query ["SELECT v.vendor_id, v.profile_pic, v.summary, u.name_first, u.name_last
                  FROM Vendors v
                  INNER JOIN Users u
                  ON v.user_id = u.user_id
                  WHERE v.vendor_id = ?" vendor-id])
      first))

(defn retrieve-ven-details
  [vendor-id]
  "assembles all the data from various tables for the ven-details response"
  (let [ven-user (get-ven-user vendor-id)
        services (db/list-services-for-vendor vendor-id)
        photos (db/list-photos-by-ven-id vendor-id false)]
    (assoc ven-user :services services
                    :photos photos)))
    
