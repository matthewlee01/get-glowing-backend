(ns globar.upload-file
   (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system! q]]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.data.json :as json]))

(use-fixtures :once setup-test-system!)

(def DEST_DIR "resources/public/images/")

(deftest test-file-upload


  ;; call the upload endpoint to upload a file
  (let [post-result (shell/sh "curl" 
                              "-X"
                              "POST"
                              "-H"
                              "content-type: multipart/form-data"
                              "localhost:8889/upload"
                              "-F"
                              "image=@dev-resources/clojure_logo.png")

        post-clj (json/read-str (:out post-result) :key-fn keyword)
        filename (:filename post-clj)]
    ;; read the destination directory for the file we uploaded

    (is (= (.exists (io/file (str  DEST_DIR filename))) true))

    ;; first we make sure the file isn't there from previous tests
    (when (.exists (io/file (str DEST_DIR filename)))
      (io/delete-file (str DEST_DIR filename)))))
