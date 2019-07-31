(ns globar.calendar.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code.
  :100 errors are calendar data problems
  :200 errors are calendar structure problems
  :000 is unknown error"
  (condp ep/includes-str? spec-str
    "failed: bookings-available?" :200
    "failed: distinct-time-chunks? in: [:booked]" :201
    "failed: (contains? % :template)" :202
    "failed: (contains? % :date)" :203
    "failed: (contains? % :available)" :204
    "failed: (contains? % :booked)" :205
    "failed: vector? in: [:available]" :206
    "failed: vector? in: [:booked]" :207
    "failed: vector? in: [:template]" :208
    (ep/common-get-error-code spec-str)))

(def ERROR_CODE_KEY
  {:200 :unavailable-booking-msg
   :201 :overlapping-bookings-msg
   :202 :invalid-data-msg
   :203 :invalid-data-msg
   :204 :invalid-data-msg
   :205 :invalid-data-msg
   :206 :invalid-data-msg
   :207 :invalid-data-msg
   :208 :invalid-data-msg})
