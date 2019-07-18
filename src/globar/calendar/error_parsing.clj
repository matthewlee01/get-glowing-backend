(ns globar.calendar.error-parsing
  (:require [clojure.string :as str]))

(defn includes-str?
  [substr str]
  "clojure.string/includes? but with the parameters switched so that it works with condp"
  (str/includes? str substr))

(def ERROR_CODE_KEY
    {:100 :unavailable-booking-msg
     :101 :overlapping-bookings-msg
     :102 :bad-date-msg
     :200 :invalid-data-msg
     :201 :invalid-data-msg
     :202 :invalid-data-msg
     :203 :invalid-data-msg
     :204 :invalid-data-msg
     :205 :invalid-data-msg
     :206 :invalid-data-msg
     :207 :invalid-data-msg
     :208 :invalid-data-msg
     :209 :invalid-data-msg
     :210 :invalid-data-msg
     :211 :invalid-data-msg
     :212 :invalid-data-msg
     :213 :invalid-data-msg
     :214 :invalid-data-msg
     :000 :unknown-error-msg})

(def ERROR_MSG_SET_EN 
  {:unavailable-booking-msg "Sorry, that booking is not available. Please try a different time."
   :overlapping-bookings-msg "Sorry, that time is already booked. Please try a different time."
   :bad-date-msg "Sorry, the date you tried to book is invalid. Please ensure that your date is formatted as YYYY-MM-DD!"
   :invalid-data-msg "Sorry, but something went wrong while registering your booking. Please take note of your error code and contact us for assistance."
   :unknown-error-msg "Sorry, an unknown error occurred. Please try again, or contact us if the problem persists."})
    
(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code.
  :100 errors are calendar data problems
  :200 errors are calendar structure problems
  :000 is unknown error"
  (condp includes-str? spec-str
    "failed: bookings-available?" :100
    "failed: distinct-time-chunks? in: [:booked]" :101
    "failed: (re-matches DATE_REGEX %)" :102
    "failed: map?" :200
    "failed: (contains? % :template)" :201
    "failed: (contains? % :date)" :202
    "failed: (contains? % :available)" :203
    "failed: (contains? % :booked)" :204
    "failed: vector? in: [:available]" :205
    "failed: vector? in: [:booked]" :206
    "failed: vector? in: [:template]" :207
    "failed: string? in: [:date]" :208
    "failed: (< (first %) (second %))" :209
    "failed: (= (count %) 2)" :210
    "failed: (< % MAX_TIME)" :211
    "failed: (>= % MIN_TIME)" :212
    "spec: :globar.calendar.core/time" :213
    "spec: :globar.calendar.core/time-chunk" :214
    :000))

(defn get-error-data
  [error-msg-map spec-str]
  "takes a spec error data string and an error message set and converts it to info for the client to display"
  (let [error-code (get-error-code spec-str)
        error-key (error-code ERROR_CODE_KEY)
        error-msg (error-key error-msg-map)]
    {:code error-code
     :message error-msg}))
