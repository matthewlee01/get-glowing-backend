(ns globar.ven-list.core
  (:require [globar.ven-list.db :as vl-db]
            [io.pedestal.http :as http]))

(defn get-ven-list-page
  [request]
  (let [token (get-in request [:json-params :token])
        {:keys [vendor-id sort-by filter-by page-size]} token
        raw-page (vl-db/get-ven-page vendor-id (inc page-size) sort-by filter-by)
        last-page? (<= (count raw-page) page-size)
        actual-page (if (not last-page?)
                      (drop-last raw-page)
                      raw-page)]
    (http/json-response {:data {:page actual-page
                                :last-page? last-page?}})))
        
    
        

