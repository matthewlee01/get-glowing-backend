(ns globar.ven-reg.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code."
  (condp ep/includes-str? spec-str
    ;; :400: vendor registration data problems
    "failed: map?" :400
    "failed: (contains? % :name-first)" :401
    "failed: (contains? % :name-last)" :402
    "failed: (contains? % :email)" :403
    "failed: (contains? % :phone)" :404
    "failed: (contains? % :addr-state)" :405
    "failed: (contains? % :addr-city)" :406
    "failed: (contains? % :addr-str-name)" :407
    "failed: (contains? % :addr-str-num)" :408
    "failed: (contains? % :addr-postal)" :409
    "failed: string? in: [:name-first]" :410
    "failed: string? in: [:name-last]" :411
    "failed: string? in: [:email]" :412
    "failed: string? in: [:addr-str-name]" :413
    "failed: string? in: [:addr-city]" :414
    "failed: string? in: [:addr-state]" :415
    "failed: string? in: [:addr-postal]" :416
    "failed: int? in: [:addr-str-num]" :417
    "failed: int? in: [:vendor-id]" :418
    "failed: int? in: [:phone]" :419
    "failed: find-vendor-by-id" :420
    ;; :000: unknown error, ideally this should never happen
    :000))


