(ns globar.users.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  (condp ep/includes-str? spec-str
    "failed: string? in: [:email]" :600
    "failed: string? in: [:name-first]" :601
    "failed: string? in: [:name-last]" :602
    "failed: string? in: [:name]" :603
    "failed: string? in: [:avatar]" :604
    "failed: string? in: [:locale]" :605
    "failed: string? in: [:sub]" :606
    "failed: (re-matches EMAIL-REGEX %)" :607
    "spec: :globar.users.core/email-verified" :608
    "spec: :globar.users.core/is-vendor" :609
    (ep/common-get-error-code spec-str)))

(def ERROR_CODE_KEY
  {:600 :invalid-data-msg
   :601 :invalid-data-msg
   :602 :invalid-data-msg
   :603 :invalid-data-msg
   :604 :invalid-data-msg
   :605 :invalid-data-msg
   :606 :invalid-data-msg
   :607 :bad-email-msg
   :608 :invalid-data-msg
   :609 :invalid-data-msg})
  
