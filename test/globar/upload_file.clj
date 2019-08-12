(ns globar.upload-file
   (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system! q]]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.data.json :as json]
            [ring.util.codec :as codec])) 

(use-fixtures :once setup-test-system!)

(def DEST_DIR "resources/public/images/")

(deftest test-file-upload
  ;; call the upload endpoint to upload a file
  (let [desc "hello world & my name is \\"
        token "uh oh broken. need magic debug token logic here"
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
                              (str "description=" (codec/url-encode desc)))

        post-clj (json/read-str (:out post-result) :key-fn keyword)
        filename (:filename post-clj)]
    ;; read the destination directory for the file we uploaded
    (is (= (.exists (io/file (str DEST_DIR filename))) true))

    ;; read the db to check for the corresponding row
    (let [images (globar.images.db/get-images 1234)
          image (first images)]
      (is some? image)
      (is (= (:vendor-id image) 1234))
      (is (= (:service-id image) 5))
      (is (= (:description image) desc))
      (is (= (:filename image) filename)))

    ;; first we make sure the file isn't there from previous tests
    (when (.exists (io/file (str DEST_DIR filename)))
      (io/delete-file (str DEST_DIR filename)))))

(deftest test-json-file-upload
  (let [file (io/file "dev-resources/clojure_logo.png")
        body {:filename "clojure_logo.png"
              :file file}]))
    
