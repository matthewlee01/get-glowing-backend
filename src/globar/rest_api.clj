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
            [fipp.edn :refer [pprint]]
            [clojure.spec.alpha :as s])
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
    (http/json-response (cc/write-calendar-day vendor-id body-data))))

(defn get-calendar
  [request]
  (let [vendor-id (read-string (get-in request [:path-params :vendor-id]))
        date (str (get-in request [:path-params :date]))]
    (log/debug :rest-fn :get-calendar :vendor-id vendor-id :date date)
    (http/json-response (cc/read-calendar vendor-id date))))

(defn upsert-vendor 
  [request]
  (let [vendor (get-in request [:json-params])
        vendor-id (:vendor-id vendor)]
    (if (nil? vendor-id)
      (http/json-response (db/create-vendor vendor))
      (do (db/update-vendor vendor)
          (http/json-response (db/find-vendor-by-id vendor-id))))))

(defn upsert-user
  [request]
  (let [user (get-in request [:json-params])]
    (if (:user-id user)
      (do (db/update-user user)
          (http/json-response (db/find-user-by-id (:user-id user))))
      (http/json-response (db/create-user user)))))

;only works for writing, updating is wip
(defn upsert-booking
  [request]
  (let [{:keys [vendor-id time booking-id date] :as booking} (:json-params request)
        cal-day (get-in (cc/read-calendar vendor-id date) [:day-of :calendar])
        new-cal-day (-> (cc/insert-booking cal-day time)
                        (assoc :date date))]
    (if (s/valid? ::cc/valid-calendar new-cal-day)
      (if (nil? booking-id) ;;checks if booking already exists, then creates/updates accordingly
        (do (cc/write-calendar-day vendor-id new-cal-day)
            (http/json-response (db/create-booking booking)))
        (http/json-response (db/update-booking booking))) ;;update functionality needs to be expanded
      (http/json-response {:error (s/explain-str ::cc/valid-calendar new-cal-day)
                           :date date}))))

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

(defn login
  "write the last login time for this user, and
  create the user record if it doesn't already exist"
  [request]

  ;; first need to validate the signature on this token
  (let [access-token (get-in request [:json-params :token])
        token-valid? (verify access-token)]
    (if token-valid?
      (do
        (let [access-token (get-in request [:json-params :token])
              a0 (new AuthAPI (:domain config/auth0)
                              (:client-id config/auth0)
                              (:client-secret config/auth0))
              user-info (->> (.userInfo a0 access-token)
                             (.execute)
                             (.getValues)
                             (into {})
                             (clojure.walk/keywordize-keys))
              user (db/find-user-by-sub (:sub user-info))]

          ; if the user doesn't exist, create a new user record
          (when-not user
            ;; change the name from auth0 from picture->avatar as that's what we use
            (db/create-user (clojure.set/rename-keys user-info {:picture :avatar
                                                                :given_name :name-first
                                                                :family_name :name-last})))

          ; read the user info and send it back to the client
          (http/json-response (db/find-user-by-sub (:sub user-info)))))
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

(defroutes rest-api-routes
  [[["/upload" ^:interceptors [(ring-mw/multipart-params)] {:post upload}]
    ["/calendar/:vendor-id" ^:interceptors [(body-params/body-params)] {:post put-calendar}]
    ["/calendar/:vendor-id/:date" {:get get-calendar}]
    ["/vendor" ^:interceptors [(body-params/body-params)] {:post upsert-vendor}]
    ["/user" ^:interceptors [(body-params/body-params)] {:post upsert-user}]
    ["/login" ^:interceptors [(body-params/body-params)] {:post login}]
    ["/booking" ^:interceptors [(body-params/body-params) authorization-interceptor] {:post upsert-booking}]]])
