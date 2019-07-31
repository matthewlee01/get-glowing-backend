(ns globar.bookings.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code.
  :100 errors are calendar data problems
  :200 errors are calendar structure problems
  :000 is unknown error"
  (condp ep/includes-str? spec-str
    ;; :500: booking-specific data problems
    "failed: (contains % :user-id)" :500
    "failed: (contains % :vendor-id)" :501
    "failed: (contains % :service)" :502
    "failed: (contains % :time)" :503
    "failed: (contains % :date)" :504
    "failed: int? in: [:booking-id]" :505
    "failed: int? in: [:service]" :506
    "failed: find-booking-by-id" :507
    (ep/common-get-error-code spec-str)))

(def ERROR_CODE_KEY
  {:500 :invalid-data-msg
   :501 :invalid-data-msg
   :502 :invalid-data-msg
   :503 :invalid-data-msg
   :504 :invalid-data-msg
   :505 :invalid-data-msg
   :506 :invalid-data-msg
   :507 :booking-not-found-msg})
