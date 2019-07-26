(ns globar.booking-tests
  (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system!]]
            [globar.db :as db]
            [globar.rest-api :as r-a]))


(def booking-call {:vendor-id 1234 :user-id 1410 :time [0 1] :date "2019-07-18"})
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
  
  
