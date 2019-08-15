(ns globar.images.core
  (:require [globar.config :as config]
            [globar.db :as db]
            [clojure.edn :as edn]
            [globar.images.db :as image-db]
            [io.pedestal.http :as http]
            [clojure.java.io :as io]))

(defn stream->bytes [input-stream]
  (loop [buffer (.read input-stream) accum []]
    (if (< buffer 0)
      accum
      (recur (.read input-stream) (conj accum buffer)))))

(defn upload 
  [request]
  "upload a file - should be a photo?"
  (let [form-params (:params request)
        vendor-id (get-in request [:vendor :vendor-id])
        src-file (get form-params (:file-field config/image-config))
        service-id (edn/read-string (get form-params (:service-id-field config/image-config)))
        desc (get form-params (:description-field config/image-config))
        src-filename (:filename src-file)
        src-filetype (last (clojure.string/split src-filename #"\."))
        new-filename (str (java.util.UUID/randomUUID) "." src-filetype)
        input-file (:tempfile src-file)
        file-bytes (with-open [input-stream (io/input-stream input-file)]
                     (stream->bytes input-stream))]
    ;; copy the file to the destination
    (io/copy input-file (io/file (:dest-dir config/image-config) new-filename))
    ;; write a new row to the images table
    (image-db/create-image {:deleted false :published false :vendor-id vendor-id :service-id service-id :description desc :filename new-filename})    
    (http/json-response {:filename new-filename})))

(defn v-get-photos
  [request]
  (let [vendor-id (get-in request [:vendor :vendor-id])
        photos (db/list-photos-by-ven-id vendor-id true)]
    (http/json-response {:photos photos})))

(defn v-publish-photo
  [request]
  (let [vendor-id (get-in request [:vendor :vendor-id])
        filename (get-in request [:json-params :filename])]
    (if (= filename "all")
      (image-db/ven-publish-all vendor-id)
      (image-db/update-image {:filename filename :published true}))
    (v-get-photos request)))

(defn v-delete-photo
  [request]
  (let [filename (get-in request [:json-params :filename])]
    (image-db/update-image {:filename filename :deleted true})
    (v-get-photos request)))


