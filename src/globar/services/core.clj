(ns globar.services.core
  (:require [clojure.spec.alpha :as s]
            [globar.db :as db]
            [globar.error-parsing :as ep]
            [globar.services.db :as s-db]
            [globar.services.error-parsing :as s-ep]
            [globar.specs :as common-specs]
            [io.pedestal.http :as http]))

(s/def ::vendor-id
  ::common-specs/vendor-id)

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
  (let [vendor (get-in request [:vendor])]
    (http/json-response (db/list-services-for-vendor (:vendor-id vendor)))))

(defn upsert-service [request]
  "Create or modify a service definition record"
  (let [vendor (:vendor request) 
        service (-> (get-in request [:json-params :service])
                    (assoc :vendor-id (:vendor-id vendor)))]
    (if (s/valid? ::valid-service service)  
     (http/json-response (if (:service-id service) 
                           (s-db/update-service service)
                           (s-db/create-service service)))
     (http/json-response {:error (->> service
                                      (s/explain-str ::valid-service)
                                      (ep/get-error-data ep/ERROR_MSG_SET_EN s-ep/get-error-code))}))))




