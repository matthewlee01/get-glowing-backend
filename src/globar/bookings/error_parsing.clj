(ns globar.bookings.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code.
  :100 errors are calendar data problems
  :200 errors are calendar structure problems
  :000 is unknown error"
  (condp ep/includes-str? spec-str
    :000))

