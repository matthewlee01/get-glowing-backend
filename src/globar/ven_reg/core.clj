(ns globar.ven-reg.core
  (:require [clojure.spec.alpha :as s]
            [globar.db :as db]
            [globar.error-parsing :as ep]
            [globar.ven-reg.error-parsing :as vr-ep]
            [globar.ven-reg.db :as vr-db]
            [io.pedestal.http :as http]))

(s/def ::vendor-id
  (s/and integer?
         db/find-vendor-by-id))

(s/def ::name-first  ;;describes a valid time value
  string?)

(s/def ::name-last
  string?)  

(s/def ::email ;;describes a valid time collection
  string?) 

(s/def ::addr-street
  string?)

(s/def ::addr-city
  string?)

(s/def ::addr-state
  string?)

(s/def ::addr-postal
  string?)

(s/def ::phone
  integer?)
         
(s/def ::valid-vendor-for-creation
  (s/keys :req-un [::name-first ::name-last ::email ::addr-street ::addr-city ::addr-state ::addr-postal ::phone]))

(s/def ::valid-vendor-for-update
  (s/keys :req-un [::vendor-id]
          :opt-un [::name-first ::name-last ::email ::addr-street ::addr-city ::addr-state ::addr-postal ::phone]))

(defn upsert-vendor 
  [request]
  (let [vendor (get-in request [:json-params])
        vendor-id (:vendor-id vendor)]
    (if (nil? vendor-id)
      (if (s/valid? ::valid-vendor-for-creation vendor)
        (http/json-response (vr-db/create-vendor vendor))
        (http/json-response {:error (->> vendor
                                         (s/explain-str ::valid-vendor-for-creation)
                                         (ep/get-error-data ep/ERROR_MSG_SET_EN vr-ep/get-error-code vr-ep/ERROR_CODE_KEY))}))
      (if (s/valid? ::valid-vendor-for-update vendor)
        (do (vr-db/update-vendor vendor)
            (http/json-response (db/find-vendor-by-id vendor-id)))
        (http/json-response {:error (->> vendor
                                         (s/explain-str ::valid-vendor-for-update)
                                         (ep/get-error-data ep/ERROR_MSG_SET_EN vr-ep/get-error-code vr-ep/ERROR_CODE_KEY))})))))
