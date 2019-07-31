(ns globar.services.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code."
  (condp ep/includes-str? spec-str
    ;; :300: service data problems
    "failed: (contains? % :s-name)" :300
    "failed: (contains? % :s-type)" :301
    "failed: (contains? % :s-description)" :302
    "failed: (contains? % :s-duration)" :303
    "failed: (contains? % :s-price)" :304
    "failed: (contains? % :vendor-id)" :305
    "failed: string? in: [:s-type]" :306
    "failed: string? in: [s-description]" :307
    "failed: string? in: [s-name]" :308
    "failed: integer? in: [:s-duration]" :309
    "failed: integer? in: [:s-price]" :310
    "failed: pos? in: [:s-duration]" :311
    "failed: pos? in: [:s-price]" :312
    (ep/common-get-error-code spec-str)))

(def ERROR_CODE_KEY
  {:300 :invalid-data-msg
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
   :312 :invalid-data-msg})
