(ns globar.specs
  (:require [clojure.spec.alpha :as s]
            [globar.db :as db]))

(s/def ::user-id
  (s/and integer?
         db/find-user-by-id))

(s/def ::vendor-id
  (s/and integer?
         db/find-vendor-by-id))

