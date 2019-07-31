(ns globar.error-parsing
  (:require [clojure.string :as str]))

(defn includes-str?
  [substr str]
  "clojure.string/includes? but with the parameters switched so that it works with condp"
  (str/includes? str substr))

(defn common-get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code. this is the common one that contains some general patterns that various specs may have"
  (condp includes-str? spec-str
    ;;some problems with commonly used data
    "failed: map?" :100
    "failed: (re-matches DATE_REGEX %)" :101
    "failed: string? in: [:date]" :102
    "failed: int? in: [:user-id]" :103
    "failed: int? in: [:vendor-id]"  :104   
    "failed: find-vendor-by-id" :105
    "failed: find-user-by-id" :106
    ;;some common calendar data problems
    "failed: (< (first %) (second %))" :107
    "failed: (= (count %) 2)" :108
    "failed: (< % MAX_TIME)" :109
    "failed: (>= % MIN_TIME)" :110
    "spec: :globar.calendar.core/time" :111 
    "spec: :globar.calendar.core/time-chunk" :112
    ;;unknown error is default, ideally this never happens
    :000))
   
(def COMMON_CODE_KEY
    {:100 :invalid-data-msg     
     :101 :bad-date-msg
     :102 :bad-date-msg
     :103 :invalid-data-msg
     :104 :invalid-data-msg
     :105 :vendor-not-found-msg
     :106 :user-not-found-msg
     :107 :invalid-data-msg
     :108 :invalid-data-msg
     :109 :invalid-data-msg
     :110 :invalid-data-msg
     :111 :invalid-data-msg
     :112 :invalid-data-msg
     :000 :unknown-error-msg})

(def ERROR_MSG_SET_EN 
  {:unavailable-booking-msg "Sorry, that booking is not available. Please try a different time."
   :overlapping-bookings-msg "Sorry, that time is already booked. Please try a different time."
   :bad-date-msg "Sorry, the date you tried to book is invalid. Please ensure that your date is formatted as YYYY-MM-DD!"
   :invalid-data-msg "Sorry, but something went wrong while registering your data. Please take note of your error code and contact us for assistance."
   :vendor-not-found-msg "Sorry, but the vendor you are trying to access couldn't be found. Please try again, or contact us if the problem persists."
   :user-not-found-msg "Sorry, but the user you are trying to access couldn't be found. Please try again, or contact us if the problem persists."
   :booking-not-found-msg "Sorry, but the booking you are trying to access could'nt be found. Please try again, or contact us if the problem persists."
   :unknown-error-msg "Sorry, an unknown error occurred. Please try again, or contact us if the problem persists."})

(defn get-error-data
  [error-msg-map error-parser codemap spec-str]
  "takes a spec error data string, an error parser, a code map, and an error message set and converts it to info for the client to display"
  (let [error-code (error-parser spec-str)
        error-key (error-code (merge COMMON_CODE_KEY codemap))
        error-msg (error-key error-msg-map)]
    {:code error-code
     :message error-msg}))
