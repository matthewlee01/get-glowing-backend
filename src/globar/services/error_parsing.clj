(ns globar.services.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code."
  (condp ep/includes-str? spec-str
    ;; :300: service data problems
    "failed: map?" :300
    "failed: (contains? % :s-name)" :301
    "failed: (contains? % :s-type)" :302
    "failed: (contains? % :s-description)" :303
    "failed: (contains? % :s-duration)" :304
    "failed: (contains? % :s-price)" :305
    "failed: (contains? % :vendor-id)" :306
    "failed: string? in: [:s-type]" :307
    "failed: string? in: [s-description]" :308
    "failed: string? in: [s-name]" :309
    "failed: integer? in: [vendor-id]" :310
    "failed: integer? in: [:s-duration]" :311
    "failed: integer? in: [:s-price]" :312
    "failed: pos? in: [:s-duration]" :313
    "failed: pos? in: [:s-price]" :314
    "failed: find-vendor-by-id" :315
    ;; :000: unknown error, ideally this should never happen
    :000))


