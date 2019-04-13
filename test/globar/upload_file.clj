(ns globar.upload-file
   (:require [clojure.test :refer :all]
            [globar.test_utils :refer [start-test-system! stop-test-system! q]]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))


(def DEST_FILENAME "resources/public/clojure_logo.png")

(deftest test-file-upload

  ;; first we make sure the file isn't there from previous tests
  (when (.exists (io/file DEST_FILENAME))
    (io/delete-file DEST_FILENAME))

  ;; start the system
  (start-test-system!)
  (try

    ;; call the upload endpoint to upload a file
    (shell/sh "curl" 
              "-X"
              "POST"
              "-H"
              "content-type: multipart/form-data"
              "localhost:8889/upload"
              "-F"
              "image=@dev-resources/clojure_logo.png")

    ;; read the destination directory for the file we uploaded
    (is (= (.exists (io/file DEST_FILENAME)) true))

    (catch Exception e)
    (finally (stop-test-system!))))

