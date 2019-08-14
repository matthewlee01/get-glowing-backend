(ns globar.services.core
  (:require [clojure.spec.alpha :as s]
            [globar.db :as db]
            [globar.error-parsing :as ep]
            [globar.services.db :as s-db]
            [globar.services.error-parsing :as s-ep]
            [globar.ven-reg.core :as vr-c]
            [io.pedestal.http :as http]
            [io.pedestal.log :as log]))

(s/def ::vendor-id
  ::vr-c/vendor-id)

(s/def ::s-type
  string?)

(s/def ::s-price
  (s/and integer?
         pos?))

(s/def ::s-duration
  (s/and integer?
         pos?))

(s/def ::s-description
  string?)

(s/def ::s-name
  string?)
         
(s/def ::valid-service
  (s/keys :req-un [::vendor-id ::s-name ::s-description ::s-price ::s-duration ::s-type]))

(defn get-services [request]
  "Returns the services for the vendor who issued the request"
  (let [vendor (get-in request [:vendor])
        result (db/list-services-for-vendor (:vendor-id vendor))]
    (log/debug :function "get-services" :result result)
    (http/json-response result)))

(defn upsert-service [request]
  "Create or modify a service definition record"
  (let [vendor (:vendor request) 
        service (-> (get-in request [:json-params :service])
                    (assoc :vendor-id (:vendor-id vendor)))]
    (log/debug :function "upsert-service" :service service)
    (if (s/valid? ::valid-service service)  
      (do 
        (let [result (if (= 0 (:service-id service)) 
                       (s-db/create-service (dissoc service :service-id))
                       (s-db/update-service service))]

          (log/debug :function "upsert-service" :result result) 
          (http/json-response {:data result})))
     (do
       (let [error-str (s/explain-str service ::valid-service)]
         (log/debug :function "upsert-service" :error-string error-str)
         (http/json-response {:error (ep/get-error-data error-str ep/ERROR_MSG_SET_EN s-ep/get-error-code s-ep/ERROR_CODE_KEY)}))))))




