(ns globar.ven-reg.db
  (:require [clojure.java.jdbc :as jdbc]
            [globar.db :as db]
            [globar.users.db :as u-db]))

(defn create-vendor
  [new-vendor]
  (let [{:keys [user-id 
                summary 
                profile-pic]} new-vendor
        updated-user (-> (db/find-user-by-id user-id)
                         (merge new-vendor)
                         (assoc :is-vendor true) ;; set the vendor flag on the associated user object
                         (u-db/update-user))
        initial-profile-pic (if profile-pic
                              profile-pic
                              "default-pfp.png")
        result (jdbc/insert! db/db-conn :Vendors {:user_id user-id
                                                  :summary summary
                                                  :profile_pic initial-profile-pic}
                             {:identifiers #(.replace % \_\-)})]

    (first result)))

(defn update-vendor
  [vendor]
  (let [updated-vendor (-> (db/find-vendor-by-id (:vendor-id vendor))
                           (merge vendor))
        rekeyed-vendor (clojure.set/rename-keys updated-vendor {:vendor-id :vendor_id
                                                                :user-id :user_id
                                                                :profile-pic :profile_pic
                                                                :created-at :created_at
                                                                :updated-at :updated_at})
        result (jdbc/update! db/db-conn 
                              :Vendors 
                              rekeyed-vendor
                              ["vendor_id = ?" (:vendor-id vendor)])]
    (if (= 0 (first result))
      (println "ERROR UPDATING VENDOR")
      updated-vendor)))
    
