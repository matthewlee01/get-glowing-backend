(ns globar.ven-reg.core
  (:require [clojure.spec.alpha :as s]))

(s/def ::vendor-id
  int?)

(s/def ::name-first  ;;describes a valid time value
  string?)

(s/def ::name-last
  string?)  

(s/def ::email ;;describes a valid time collection
  string?) 

(s/def ::addr-str-num
  int?)

(s/def ::addr-str-name
  string?)

(s/def ::addr-city
  string?)

(s/def ::addr-state
  string?)

(s/def ::addr-postal
  string?)

(s/def ::phone
  int?)

         
(s/def ::valid-vendor
  (s/keys :req-un [::name-first ::name-last ::email ::addr-str-num ::addr-str-name ::addr-city ::addr-state ::addr-postal ::phone]
          :opt-un [::vendor-id]))