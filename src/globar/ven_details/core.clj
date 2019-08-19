(ns globar.ven-details.core
  (:require [io.pedestal.http :as http]
            [globar.ven-details.db :as vd-db]))

(defn get-ven-details
  [request]
  (let [vendor-id (get-in request [:json-params :vendor-id])
        ven-details (vd-db/retrieve-ven-details vendor-id)]
    (http/json-response ven-details)))
    
