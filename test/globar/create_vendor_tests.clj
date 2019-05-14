(ns globar.create-vendor-tests
   (:require [clojure.test :refer :all]
             [globar.test_utils :refer [q setup-test-system!]]
             [clojure.java.shell :refer [sh]]
             [clojure.data.json :as json]))

(use-fixtures :once setup-test-system!)

(deftest test-vendor-creation
  ;; first create a new vendor
  (let [new-vendor {:name_first "I am the Test user"
                    :name_last "Andrrson-Coopers"
                    :email "test@test.com"
                    :addr_str_num "#2-5985"
                    :addr_str_name "Canada Way SE"
                    :addr_city "New Westminster"
                    :addr_state "BC"
                    :addr_postal "V5e-3N9"
                    :phone "778-994-6836"
                    :locale "Pacific Standard Time"
                    :summary "This is my story - I wish I had one, but this is all I got!"}

        ;; issue the curl call
        post-result (sh "curl" "-H" 
                        "Content-Type: application/json" 
                        "-d" (json/write-str new-vendor) "-X" 
                        "POST" "http://localhost:8889/createvendor")
        post-clj (json/read-str (:out post-result) :key-fn keyword)]
     (is (= new-vendor (dissoc  post-clj 
                               :created_at 
                               :radius 
                               :profile_pic 
                               :updated_at 
                               :vendor_id 
                               :password)))))


