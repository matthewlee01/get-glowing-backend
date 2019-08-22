(ns globar.ven-profile.core 
  (:require [globar.db :as db]
            [io.pedestal.http :as http]
            [globar.users.db :as u-db]
            [globar.ven-reg.db :as v-db]
            [globar.ven-profile.db :as vp-db]))

(defn v-get-profile
  [request]
  (let [vendor-id (get-in request [:vendor :vendor-id])
        ven-profile (vp-db/get-full-vendor-profile vendor-id)
        services (db/list-services-for-vendor vendor-id)
        images (db/list-images-by-ven-id vendor-id true)]
    (http/json-response ven-profile)))
    
(defn v-update-profile
  [request]
  (clojure.pprint/pprint request)
  (let [vendor-id (get-in request [:vendor :vendor-id])
        updated-vals (get-in request [:json-params :updated-vals])
        user-id (db/find-user-id-by-ven-id vendor-id)
        vendor-map (-> (select-keys updated-vals [:summary
                                                  :profile-pic])
                       (assoc :vendor-id vendor-id))
        user-map (-> (select-keys updated-vals [:locale
                                                :addr-street
                                                :addr-city
                                                :addr-state
                                                :addr-postal
                                                :name-first
                                                :name-last
                                                :phone
                                                :email])
                     (assoc :user-id user-id))
        user-result (if (> (count user-map) 1) 
                      (u-db/update-user user-map)
                      (db/find-user-by-id user-id))
        vendor-result (if (> (count vendor-map) 1)
                        (v-db/update-vendor vendor-map)
                        (db/find-vendor-by-id vendor-id))]
    (http/json-response (-> (merge user-result vendor-result)
                            (select-keys [:summary
                                          :profile-pic
                                          :locale
                                          :addr-street
                                          :addr-city
                                          :addr-state
                                          :addr-postal
                                          :name-first
                                          :name-last
                                          :phone
                                          :email])))))
