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
    (let [images (globar.db/list-photos-by-ven-id 1234 true)
          image (first images)]
      (is some? image)
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
    
