(ns globar.rest-api
  (:require [clojure.java.io :as io]
            [globar.error-parsing :as ep]
            [globar.calendar.error-parsing :as c-ep]
            [globar.ven-reg.error-parsing :as v-ep]
            [globar.users.error-parsing :as u-ep]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.http.ring-middlewares :as ring-mw]
            [io.pedestal.log :as log]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http :as http]
            [io.pedestal.interceptor.helpers :refer [defbefore]]
            [globar.config :as config]
            [globar.bookings.core :as b-c]
            [globar.calendar.core :as c-c]
            [globar.services.core :as s-c]
            [globar.users.core :as u-c]
            [globar.db :as db]
            [globar.users.db :as u-db]
            [globar.ven-reg.core :as vr-c]
            [globar.ven-reg.db :as vr-db]
            [cheshire.core :as cheshire]
            [buddy.core.keys :as keys]
            [fipp.edn :refer [pprint]]
            [clojure.spec.alpha :as s]
            [globar.images.db :as image-db]
            [ring.util.codec :as codec])
  (:import
            [java.util Base64]
            [com.auth0.jwt JWT]
            [com.auth0.jwt.algorithms Algorithm]
            [com.auth0.client.auth AuthAPI]))


(defn stream->bytes [input-stream]
  (loop [buffer (.read input-stream) accum []]
    (if (< buffer 0)
      accum
      (recur (.read input-stream) (conj accum buffer)))))

(defn upload 
  [request]
  "upload a file - should be a photo?"
  (let [form-params (:params request)
        src-file (get form-params (:file-field config/image-config))
        service-id (Integer/parseInt (get form-params (:service-id-field  config/image-config)))
        desc (codec/url-decode (get form-params (:description-field config/image-config)))
        src-filename (:filename src-file)
        src-filetype (last (clojure.string/split src-filename #"\."))
        new-filename (str (java.util.UUID/randomUUID) "." src-filetype)
        input-file (:tempfile src-file)
        file-bytes (with-open [input-stream (io/input-stream input-file)]
                     (stream->bytes input-stream))]
    ;; copy the file to the destination
    (io/copy input-file (io/file (:dest-dir config/image-config) new-filename))
    ;; write a new row to the images table
    (image-db/create-image {:vendor-id 1234 :service-id service-id :description desc :filename new-filename})    
    (http/json-response {:filename new-filename})))

(defn decode64 [str]
  (String. (.decode (Base64/getDecoder) str)))

(defn encode64 [bytes]
  (.encodeToString (Base64/getEncoder) bytes))

(defonce pub-key (keys/public-key "pubkey.pem"))

(defn verify [token]
  "verifies the JWT from auth0, returns an identity map if valid
  nil otherwise - note this references a global var pub-key"
  (try
    (let [alg (Algorithm/RSA256 pub-key nil)
          verifier (-> (JWT/require alg)
                       (.withIssuer (into-array ["https://n00b.auth0.com/"]))
                       (.build))
          decoded-jwt (.verify verifier token)
          payload (-> decoded-jwt
                      (.getPayload)
                      (decode64)
                      (cheshire/parse-string true))]

      payload) 
    (catch Exception e
      (log/debug ::verify "failed to verify incoming user token" :token token)
      nil)))

(defn userinfo-from-token [token]
  "call auth0 to get user-info based on the id specified by an access token"
  (let [a0 (new AuthAPI (:domain config/auth0)
                        (:client-id config/auth0)
                        (:client-secret config/auth0))
        user-info (->> (.userInfo a0 token)
                       (.execute)
                       (.getValues)
                       (into {})
                       clojure.walk/keywordize-keys)]
    user-info))

(defn login
  "write the last login time for this user, and
  create the user record if it doesn't already exist"
  [request]
  ;; first need to validate the signature on this token
  (let [access-token (get-in request [:json-params :token])]
    (if-let [token-valid? (verify access-token)]
      (let [user-info (userinfo-from-token access-token)
            user (db/find-user-by-sub (:sub user-info))]

          ; if the user doesn't exist, create a new user record
          (when-not user
              ;; change the name from auth0 from picture->avatar as that's what we use
              (let [new-user (-> user-info
                               (clojure.set/rename-keys {:picture :avatar
                                                         :given_name :name-first
                                                         :family_name :name-last})
                               (assoc :is-vendor false))]
                (if (s/valid? ::u-c/valid-user new-user)
                  (u-db/create-user new-user)
                  (http/json-response {:error (->> new-user
                                                   (s/explain-str ::u-c/valid-user)
                                                   (ep/get-error-data ep/ERROR_MSG_SET_EN u-ep/get-error-code u-ep/ERROR_CODE_KEY))}))))

          ; now that the user exists in our db either way, 
          ; read the user and send it back to the client
          (http/json-response (db/find-user-by-sub (:sub user-info))))

       
      ;; the call to verify failed so fail the login
      {:status 403})))

(defn find-user-from-token [token]
  "given an access token, return the associated user in our system"
  [token]
  (when-let [payload (verify token)]
      (db/find-user-by-sub (:sub payload))))

;; this interceptor is responsible for ensuring that the token passed as part of the
;; request is valid, and that the user-id of the token matches that found in the request
(def authorization-interceptor
  {:name ::authorization-interceptor
   :enter (fn [context]
            (let [access-token (get-in context [:request :json-params :access-token])
                  user-id (get-in context [:request :json-params :user-id])
                  token-user (find-user-from-token access-token)]

              ;; this fancy statement check two conditions: none of the access-token, user-id or token-user are nil
              ;; and that the user specified in the token is the same user specified in the request
              (if (and (every? some? [access-token user-id token-user])
                       (= (:user-id token-user) user-id)) 
                (do
                  (log/debug ::authorization-interceptor "ACCESS GRANTED")
                  context)
                (do
                  (log/debug ::authorization-interceptor "ACCESS DENIED")
                  (assoc context :response {:status 403
                                            :headers {}
                                            :body {}})))))})

(def ven-authz-interceptor
  {:name ::ven-authz-interceptor
   :enter (fn [context]
            (let [access-token (get-in context [:request :json-params :access-token])
                  user (find-user-from-token access-token)
                  vendor (db/find-vendor-by-user (:user-id user))]

              (if vendor
                (do
                  (println "found vendor: " vendor)
                  (log/debug ::ven-authz-interceptor "ACCESS GRANTED")
                  (assoc-in context [:request :vendor] vendor))
                (do
                  (log/debug ::ven-authz-interceptor "ACCESS DENIED")
                  (println "access denied")
                  (assoc context :response {:status 403
                                            :headers {}
                                            :body {}})))))})
(defroutes rest-api-routes
  [[["/upload" ^:interceptors [(ring-mw/multipart-params)] {:post upload}]
    ["/calendar/:vendor-id" ^:interceptors [(body-params/body-params)] {:post c-c/put-calendar}]
    ["/calendar/:vendor-id/:date" {:get c-c/get-calendar}]
    ["/vendor" ^:interceptors [(body-params/body-params)] {:post vr-c/upsert-vendor}]
    ["/user" ^:interceptors [(body-params/body-params)] {:post u-c/upsert-user}]
    ["/login" ^:interceptors [(body-params/body-params)] {:post login}]
    ["/booking" ^:interceptors [(body-params/body-params) authorization-interceptor] {:post b-c/upsert-booking}]
    ["/v_calendar" ^:interceptors [(body-params/body-params) ven-authz-interceptor] {:post c-c/v-get-calendar}]
    ["/v_bookings" ^:interceptors [(body-params/body-params) ven-authz-interceptor] {:post b-c/v-get-bookings}]
    ["/services" ^:interceptors [(body-params/body-params) ven-authz-interceptor] {:post s-c/get-services}]
    ["/service" ^:interceptors [(body-params/body-params) ven-authz-interceptor] {:post s-c/upsert-service}]]])

