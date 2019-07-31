(ns globar.ven-reg.core
  (:require [clojure.spec.alpha :as s]
            [globar.db :as db]
            [globar.error-parsing :as ep]
            [globar.ven-reg.error-parsing :as vr-ep]
            [globar.specs :as common-specs]
            [globar.ven-reg.db :as vr-db]
            [io.pedestal.http :as http]))

(s/def ::vendor-id
  ::common-specs/vendor-id)

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

(defn upsert-vendor 
  [request]
  (let [vendor (get-in request [:json-params])
        vendor-id (:vendor-id vendor)]
    (if (s/valid? ::valid-vendor vendor)
        (if (nil? vendor-id)
          (http/json-response (vr-db/create-vendor vendor))
          (do (vr-db/update-vendor vendor)
              (http/json-response (db/find-vendor-by-id vendor-id))))
        (http/json-response {:error (->> vendor
                                       (s/explain-str ::valid-vendor)
                                       (ep/get-error-data ep/ERROR_MSG_SET_EN vr-ep/get-error-code vr-ep/ERROR_CODE_KEY))}))))
