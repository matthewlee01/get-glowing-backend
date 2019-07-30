(ns globar.calendar.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code.
  :100 errors are calendar data problems
  :200 errors are calendar structure problems
  :000 is unknown error"
  (condp ep/includes-str? spec-str
    ;; :100: calendar data problems
    "failed: bookings-available?" :100
    "failed: distinct-time-chunks? in: [:booked]" :101
    "failed: (re-matches DATE_REGEX %)" :102
    ;; :200: calendar structure problems
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
    ;; :000: unknown error, ideally this should never happen
    :000))


