(ns globar.services.db
  (:require [io.pedestal.log :as log]
            [clojure.java.jdbc :as jdbc]
            [globar.db :as db]))

(defn create-service
  [new-service]
  (log/debug :function-name ::create-service :new-service new-service)
  (let [field-spec (select-keys new-service [:vendor-id :s-name :s-description :s-type :s-price :s-duration])
        db-field-spec (clojure.set/rename-keys field-spec
                                               {:vendor-id :vendor_id
                                                :s-name :s_name
                                                :s-description :s_description
                                                :s-type :s_type
                                                :s-price :s_price
                                                :s-duration :s_duration})
        result (jdbc/insert! db/db-conn
                             :Services 
                             db-field-spec
                             {:identifiers #(.replace % \_\-)})]
    (first result)))

(defn update-service
  [service]
  (log/debug :function-name ::update-service :service service)
  (let [field-spec (select-keys service [:vendor-id :s-name :s-description :s-type :s-price :s-duration])
        db-field-spec (clojure.set/rename-keys field-spec
                                               {:vendor-id :vendor_id
                                                :s-name :s_name
                                                :s-description :s_description
                                                :s-type :s_type
                                                :s-price :s_price
                                                :s-duration :s_duration})
        result (jdbc/update! db/db-conn
                             :Services                              
                             db-field-spec
                             ["vendor_id = ? and service_id = ?" (:vendor-id service) (:service-id service)])]
    (println "TODO: add error checking to this!! " result)
    (if (= 0 (first result))
      (println "ERROR UPDATING SERVICE"))))