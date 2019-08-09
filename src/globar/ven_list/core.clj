(ns globar.ven-list.core
  (:require [globar.ven-list.db :as vl-db]
            [io.pedestal.http :as http]
            [clojure.spec.alpha :as s]
            [globar.error-parsing :as ep]
            [globar.ven-list.error-parsing :as vl-ep]
            [globar.ven-reg.core :as vr-c]))

(s/def ::vendor-id
  (s/nilable ::vr-c/vendor-id))

(s/def ::sort-by
  (s/and vector?
         #(= (count %) 3)
         #(string? (first %))
         (s/or :asc #(= (last %) "asc")
               :desc #(= (last %) "desc"))))

(s/def ::filter-vec
  (s/and vector?
         #(= (count %) 2)
         #(string? (first %))))

(s/def ::filter-by
  (s/and vector? 
         (s/coll-of ::filter-vec)))

(s/def ::page-size
  (s/and integer?
         pos?))

(s/def ::valid-token
  (s/keys :req-un [::vendor-id ::sort-by ::filter-by ::page-size]))

(defn get-ven-list-page
  [request]
  (let [token (get-in request [:json-params :token])]
    (if (s/valid? ::valid-token token)
      (let [{:keys [vendor-id sort-by filter-by page-size]} token
            raw-page (vl-db/get-ven-page vendor-id (inc page-size) sort-by filter-by)
            last-page? (<= (count raw-page) page-size)
            actual-page (if (not last-page?)
                          (drop-last raw-page)
                          raw-page)]
        (http/json-response {:data {:page actual-page
                                    :last-page? last-page?}}))
      (http/json-response {:error (->> token
                                      (s/explain-str ::valid-token)
                                      (ep/get-error-data ep/ERROR_MSG_SET_EN vl-ep/get-error-code vl-ep/ERROR_CODE_KEY))}))))
        
    
        

