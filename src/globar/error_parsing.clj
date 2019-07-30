(ns globar.error-parsing
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
     :300 :invalid-data-msg
     :301 :invalid-data-msg
     :302 :invalid-data-msg
     :303 :invalid-data-msg
     :304 :invalid-data-msg
     :305 :invalid-data-msg
     :306 :invalid-data-msg
     :307 :invalid-data-msg
     :308 :invalid-data-msg
     :309 :invalid-data-msg
     :310 :invalid-data-msg
     :311 :invalid-data-msg
     :312 :invalid-data-msg
     :313 :invalid-data-msg
     :314 :invalid-data-msg
     :315 :vendor-not-found-msg
     :400 :invalid-data-msg
     :401 :invalid-data-msg
     :402 :invalid-data-msg
     :403 :invalid-data-msg
     :404 :invalid-data-msg
     :405 :invalid-data-msg
     :406 :invalid-data-msg
     :407 :invalid-data-msg
     :408 :invalid-data-msg
     :409 :invalid-data-msg
     :410 :invalid-data-msg
     :411 :invalid-data-msg
     :412 :invalid-data-msg
     :413 :invalid-data-msg
     :414 :invalid-data-msg
     :415 :invalid-data-msg
     :416 :invalid-data-msg
     :417 :invalid-data-msg
     :418 :invalid-data-msg
     :419 :invalid-data-msg
     :420 :vendor-not-found-msg
     :000 :unknown-error-msg})

(def ERROR_MSG_SET_EN 
  {:unavailable-booking-msg "Sorry, that booking is not available. Please try a different time."
   :overlapping-bookings-msg "Sorry, that time is already booked. Please try a different time."
   :bad-date-msg "Sorry, the date you tried to book is invalid. Please ensure that your date is formatted as YYYY-MM-DD!"
   :invalid-data-msg "Sorry, but something went wrong while registering your data. Please take note of your error code and contact us for assistance."
   :vendor-not-found-msg "Sorry, but the vendor you are trying to access couldn't be found. Please try again, or contact us if the problem persists."
   :unknown-error-msg "Sorry, an unknown error occurred. Please try again, or contact us if the problem persists."})

(defn get-error-data
  [error-msg-map error-parser spec-str]
  "takes a spec error data string, an error parser, and an error message set and converts it to info for the client to display"
  (let [error-code (error-parser spec-str)
        error-key (error-code ERROR_CODE_KEY)
        error-msg (error-key error-msg-map)]
    {:code error-code
     :message error-msg}))
