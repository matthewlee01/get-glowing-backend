(ns globar.ven-reg.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code."
  (condp ep/includes-str? spec-str
    ;; :400: vendor registration data problems
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
    "failed: integer? in: [:addr-str-num]" :417
    "failed: integer? in: [:phone]" :418
    (ep/common-get-error-code spec-str)))

(def ERROR_CODE_KEY 
  {:400 :invalid-data-msg
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
   :418 :invalid-data-msg})
  

