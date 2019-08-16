(ns globar.upload-file
   (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system! q]]
            [clojure.java.io :as io]
            [globar.images.db :as i-db]
            [clojure.java.shell :as shell]
            [clojure.data.json :as json]))

(use-fixtures :once setup-test-system!)

(def DEST_DIR "resources/public/images/")

(deftest test-file-upload
  ;; call the upload endpoint to upload a file
  (let [desc "hello world & my name is \\"
        token "DEBUG-TEST-TOKEN"
        post-result (shell/sh "curl" 
                              "-X"
                              "POST"
                              "-H"
                              "content-type: multipart/form-data"
                              "localhost:8889/upload"
                              "-F"
                              "image=@dev-resources/clojure_logo.png"
                              "-F"
                              (str "access-token=" token)
                              "-F"
                              "service-id=5"
                              "-F"
                              (str "description=" desc))

        post-clj (json/read-str (:out post-result) :key-fn keyword)
        filename (:filename post-clj)]
    ;; read the destination directory for the file we uploaded
    (is (= (.exists (io/file (str DEST_DIR filename))) true))

    ;; read the db to check for the corresponding row
    (let [image (i-db/find-image-by-filename filename)]
      (is some? image)
      (is (= (:service-id image) 5))
      (is (= (:description image) desc))
      (let [new-description "a more interesting description"
            updated-image (i-db/update-image {:filename filename
                                              :description new-description})
            reread-image (i-db/find-image-by-filename filename)]
        (is (= (:description reread-image) new-description))
        (is (= (:filename reread-image) filename)))
    ;; first we make sure the file isn't there from previous tests
    (when (.exists (io/file (str DEST_DIR filename)))
      (io/delete-file (str DEST_DIR filename))))))
   
