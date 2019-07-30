(ns globar.bookings.db
  (:require [globar.db :as db]
            [clojure.java.jdbc :as jdbc]))

(defn create-booking
  [booking]
  (let [{:keys [vendor-id user-id time date service]} booking
        result (jdbc/insert! db/db-conn
                             :Bookings {:vendor_id vendor-id
                                        :user_id user-id
                                        :start_time (first time)
                                        :end_time (second time)
                                        :date (java.sql.Date/valueOf date)
                                        :service service
                                        :cancelled false}
                             {:identifiers #(.replace % \_\-)})]
    (first result)))

(defn update-booking
 [new-booking-info]
 (let [{:keys [booking-id]} new-booking-info
       {:keys [vendor-id user-id time date service cancelled]} (-> (db/find-booking-by-id booking-id)
                                                                   (merge new-booking-info))
       result (jdbc/update! db/db-conn
                             :Bookings {:vendor_id vendor-id
                                        :user_id user-id
                                        :start_time (first time)
                                        :end_time (second time)
                                        :date (java.sql.Date/valueOf date)
                                        :service service
                                        :cancelled cancelled}
                             ["booking_id = ?" booking-id])]
   result))
       
