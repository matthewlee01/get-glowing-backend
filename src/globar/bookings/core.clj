(ns globar.bookings.core
  (:require [io.pedestal.http :as http]
            [globar.calendar.core :as c-c]
            [globar.calendar.error-parsing :as c-ep]
            [globar.db :as db]
            [globar.bookings.db :as b-db]
            [globar.bookings.error-parsing :as b-ep]
            [globar.error-parsing :as ep]
            [globar.specs :as common-specs]
            [clojure.spec.alpha :as s]))

(s/def ::user-id
  ::common-specs/user-id)

(s/def ::vendor-id
  ::common-specs/vendor-id)

(s/def ::booking-id
  (s/and int?
         db/find-booking-by-id))

(s/def ::service
  int?)

(s/def ::time
  ::c-c/time-chunk)

(s/def ::date
  ::c-c/date)
         
(s/def ::valid-booking
  (s/keys :req-un [::user-id ::vendor-id ::service ::time ::date]
          :opt-un [::booking-id]))

(defn v-get-bookings
  [request]
  (http/json-response (db/list-bookings-for-vendor (get-in request [:vendor :vendor-id]))))

;only works for writing, updating is wip
(defn upsert-booking
  [request]
  (let [{:keys [vendor-id time booking-id date] :as booking} (:json-params request)
        cal-day (get-in (c-c/read-calendar vendor-id date) [:day-of :calendar])
        new-cal-day (-> (c-c/insert-booking cal-day time)
                        (assoc :date date))]
    (if (s/valid? ::valid-booking booking) 
      (if (s/valid? ::c-c/valid-calendar new-cal-day)
        (if (nil? booking-id) ;;checks if booking already exists, then creates/updates accordingly
          (do (c-c/write-calendar-day vendor-id new-cal-day)
              (http/json-response (b-db/create-booking booking)))
          (http/json-response (b-db/update-booking booking))) ;;update functionality needs to be expanded
        (http/json-response {:error (->> new-cal-day
                                             (s/explain-str ::c-c/valid-calendar)
                                             (ep/get-error-data ep/ERROR_MSG_SET_EN c-ep/get-error-code c-ep/ERROR_CODE_KEY))}))
      (http/json-response {:error (->> booking
                                       (s/explain-str ::valid-booking)
                                       (ep/get-error-data ep/ERROR_MSG_SET_EN b-ep/get-error-code b-ep/ERROR_CODE_KEY))}))))
