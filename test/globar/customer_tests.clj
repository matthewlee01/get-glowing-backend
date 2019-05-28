(ns globar.customer-tests
   (:require [clojure.test :refer :all]
             [globar.test_utils :refer [q setup-test-system!]]
             [clojure.java.shell :refer [sh]]
             [clojure.data.json :as json]
             [globar.db :as db]
             [fipp.edn :refer [pprint]]))

(use-fixtures :once setup-test-system!)

(deftest test-customer-creation
  ;; first create a new customer
  (let [new-customer {:name_first "I am the Test user"
                      :name_last "Andrrson-Coopers"
                      :name "Billy the Big Bad Bob"
                      :sub "social google | some funny; characters@?"
                      :avatar "http://google.com/some/url/to/a/file.jpg"
                      :email "test@test.com"
                      :email_verified true
                      :addr_str_num "#2-5985"
                      :addr_str_name "Canada Way SE"
                      :addr_city "New Westminster"
                      :addr_state "BC"
                      :addr_postal "V5e-3N9"
                      :phone "778-994-6836"
                      :locale "Pacific Standard Time"}

        ;; issue the curl call
        post-result (sh "curl" "-H" 
                        "Content-Type: application/json" 
                        "-d" (json/write-str new-customer) "-X" 
                        "POST" "http://localhost:8889/customer")
        post-clj (json/read-str (:out post-result) :key-fn keyword)]

    ;; confirm that what we wrote is what we get back from the reqeuest
    (is (= new-customer (dissoc post-clj 
                                :created_at 
                                :updated_at 
                                :cust_id 
                                :password)))))

(deftest test-customer-update
  ;; first create a new customer
  (let [customer (db/find-customer-by-id 37)
        updated  (assoc customer :password "hello mr poopy")        
        upd-cust (dissoc updated :updated_at :created_at)
        ;; issue the curl call
        post-result (sh "curl" "-H" 
                        "Content-Type: application/json" 
                        "-d" (json/write-str upd-cust) "-X" 
                        "POST" "http://localhost:8889/customer")
        post-clj (json/read-str (:out post-result) :key-fn keyword)]

    ;; confirm that what we wrote is what we get back from the reqeuest
    (is (= upd-cust (dissoc post-clj 
                            :created_at 
                            :updated_at))))) 
                            
                            
