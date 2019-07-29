(ns globar.ven-reg.db
  (:require [clojure.java.jdbc :as jdbc]
            [globar.db :as db]))

(defn create-vendor
  [new-vendor]
  (let [{:keys [user-id 
                summary 
                profile-pic]} new-vendor
        updated-user (-> (db/find-user-by-id user-id)
                         (merge new-vendor)
                         (assoc :is-vendor true) ;; set the vendor flag on the associated user object
                         (db/update-user))
        result (jdbc/insert! db/db-conn :Vendors {:user_id user-id
                                               :summary summary
                                               :profile_pic profile-pic}
                             {:identifiers #(.replace % \_\-)})]

    (first result)))

(defn update-vendor
  [vendor]
  (let [{vendor-id :vendor-id
         summary :summary} vendor
         result (jdbc/update! db/db-conn 
                              :Vendors 
                              {:summary summary}
                              ["vendor_id = ?" vendor-id])]
    (println "TODO: add error checking to this!! " result)
    (if (= 0 (first result))
      (println "ERROR UPDATING VENDOR"))))