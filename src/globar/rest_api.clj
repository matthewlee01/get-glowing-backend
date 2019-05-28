(ns globar.rest-api
  (:require [clojure.java.io :as io]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.http.ring-middlewares :as ring-mw]
            [io.pedestal.log :as log]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor.helpers :refer [defbefore]]
            [globar.config :as config]
            [globar.calendar.core :as cc]
            [globar.db :as db]
            [cheshire.core :as cheshire]
            [buddy.core.keys :as keys]
            [fipp.edn :refer [pprint]])
  (:import
            [java.util Base64]
            [com.auth0.jwt JWT]
            [com.auth0.jwt.algorithms Algorithm]
            [com.auth0.client.auth AuthAPI]))

(def FORM_PARAM "image")
(def DEST_DIR "resources/public")

(defn stream->bytes [input-stream]
  (loop [buffer (.read input-stream) accum []]
    (if (< buffer 0)
      accum
      (recur (.read input-stream) (conj accum buffer)))))

(defn upload 
  [request]
  (let [form-data (get-in request [:params FORM_PARAM])
        file-name (:filename form-data)
        input-file (:tempfile form-data)
        file-bytes (with-open [input-stream (io/input-stream input-file)]
                     (stream->bytes input-stream))]
    ;; copy the file to the destination
    (io/copy input-file (io/file DEST_DIR file-name))

    {:status 200
     :body (str "File upload was successful\n")}))

(defn put-calendar
  [request]
  (let [vendor-id (read-string (get-in request [:path-params :vendor-id]))
        body-data (get-in request [:json-params])
        date (:date body-data)
        available (:available body-data)
        updated-at (:updated-at body-data)]
    (log/debug :rest-fn :put-calendar :vendor-id vendor-id :date date :available available
               :updated-at updated-at)
    (http/json-response (cc/write-calendar vendor-id body-data))))

(defn get-calendar
  [request]
  (let [vendor-id (read-string (get-in request [:path-params :vendor-id]))
        date (str (get-in request [:path-params :date]))]
    (log/debug :rest-fn :get-calendar :vendor-id vendor-id :date date)
    (http/json-response (cc/read-calendar vendor-id date))))

(defn upsert-vendor 
  [request]
  (let [vendor (get-in request [:json-params])
        vendor-id (:vendor_id vendor)]
    (if (nil? vendor-id)
      (http/json-response (db/create-vendor vendor))
      (do (db/update-vendor vendor)
          (http/json-response (db/find-vendor-by-id vendor-id))))))

(defn upsert-customer
  [request]
  (let [customer (get-in request [:json-params])]
    (if (:cust_id customer)
      (do (db/update-customer customer)
          (http/json-response (db/find-customer-by-id (:cust_id customer))))
      (http/json-response (db/create-customer customer)))))

(defn decode64 [str]
  (String. (.decode (Base64/getDecoder) str)))

(defn encode64 [bytes]
  (.encodeToString (Base64/getEncoder) bytes))

(defonce pub-key (keys/public-key "pubkey.pem"))



(defn verify [token pub-key]
  "verifies the JWT from auth0, returns an identity map if valid
  nil otherwise"
  (let [alg (Algorithm/RSA256 pub-key nil)
        verifier (-> (JWT/require alg)
                     (.withIssuer (into-array ["https://n00b.auth0.com/"]))
                     (.build))
        decoded-jwt (.verify verifier token)
        payload (-> decoded-jwt
                    (.getPayload)
                    (decode64)
                    (cheshire/parse-string true))]

    (pprint payload)
    ;; need to add the exception handling logic around this code
    payload))

(defn login
  "write the last login time for this user, and
  create the user record if it doesn't already exist"
  [request]
  (let [access-token (get-in request [:json-params :token])
        a0 (new AuthAPI (:domain config/auth0)
                        (:client-id config/auth0)
                        (:client-secret config/auth0))
        user-info (->> (.userInfo a0 access-token)
                       (.execute)
                       (.getValues)
                       (into {})
                       (clojure.walk/keywordize-keys))
        user (db/find-customer-by-sub (:sub user-info))]

    ; if the user doesn't exist, create a new user record
    (when-not user
      (db/create-customer {:sub (:sub user-info)
                           :name_first (:given_name user-info)
                           :name_last (:family_name user-info)
                           :name (:name user-info)
                           :email (:email user-info)
                           :email_verified (:email_verified user-info)
                           :locale (:locale user-info)
                           :avatar (:picture user-info)}))

    ; read the user info and send it back to the client
    (http/json-response (db/find-customer-by-sub (:sub user-info)))))



(defroutes rest-api-routes
  [[["/upload" ^:interceptors [(ring-mw/multipart-params)] {:post upload}]
    ["/calendar/:vendor-id" ^:interceptors [(body-params/body-params)] {:post put-calendar}]
    ["/calendar/:vendor-id/:date" {:get get-calendar}]
    ["/vendor" ^:interceptors [(body-params/body-params)] {:post upsert-vendor}]
    ["/customer" ^:interceptors [(body-params/body-params)] {:post upsert-customer}]
    ["/login" ^:interceptors [(body-params/body-params)] {:post login}]]])
