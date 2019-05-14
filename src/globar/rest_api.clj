(ns globar.rest-api
  (:require [clojure.java.io :as io]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [io.pedestal.http.ring-middlewares :as ring-mw]
            [io.pedestal.log :as log]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http :as http]
            [globar.calendar.core :as cc]
            [globar.db :as db]
            [fipp.edn :refer [pprint]]))

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

(defroutes rest-api-routes
  [[["/upload" ^:interceptors [(ring-mw/multipart-params)] {:post upload}]
    ["/calendar/:vendor-id" ^:interceptors [(body-params/body-params)] {:post put-calendar}]
    ["/calendar/:vendor-id/:date" {:get get-calendar}]
    ["/vendor" ^:interceptors [(body-params/body-params)] {:post upsert-vendor}]
    ["/customer" ^:interceptors [(body-params/body-params)] {:post upsert-customer}]]])
