(ns globar.booking-tests
  (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system!]]
            [globar.db :as db]
            [globar.rest-api :as r-a]))


(def booking-call {:service 1 :vendor-id 1234 :user-id 1410 :time [0 1] :date "2019-07-18"})
(def update-call {:cancelled true})

(use-fixtures :each setup-test-system!)

(deftest create-new-bookings
  (let [{:keys [booking-id] :as booking1} (db/create-booking booking-call) ;creates new booking
        found-booking (db/find-booking-by-id booking-id) ;looks up booking based off booking id
        update-call (assoc update-call :booking-id booking-id)
        _ (db/update-booking update-call) ;updates previously made booking
        updated-booking (db/find-booking-by-id booking-id)
        cancelled? (:cancelled updated-booking)]
    (is (= (:booking-id booking1) (:booking-id found-booking)))
    (is (not= booking1 updated-booking))
    (is (= cancelled? true))))
  
(deftest test-booking-db
  (let [vendor1 1234
        vendor2 1235
        booking1 {:vendor-id vendor1 :user-id 1410 :time [0 100] :date "2019-07-18" :service 1}
        booking2 {:vendor-id vendor1 :user-id 234 :time [200 300] :date "2019-07-18" :service 1}
        booking3 {:vendor-id vendor1 :user-id 2812 :time [0 100] :date "2019-07-19" :service 1}
        booking4 {:vendor-id vendor2 :user-id 1410 :time [0 100] :date "2019-07-18" :service 2}
        booking5 {:vendor-id vendor2 :user-id 234 :time [200 300] :date "2019-07-20" :service 2}]
    ;;book some bookings in non chronological order
    (dorun (map db/create-booking [booking5 booking2 booking4 booking3 booking1]))
    (let [sorted-bookings1 (->> (db/list-bookings-for-vendor vendor1)
                                (map #(dissoc % :booking-id)))
          sorted-bookings2 (->> (db/list-bookings-for-vendor vendor2)
                                (map #(dissoc % :booking-id)))]
      ;;bookings should be sorted when read
      (is (= sorted-bookings1 [booking1 booking2 booking3]))
      (is (= sorted-bookings2 [booking4 booking5])))))  
