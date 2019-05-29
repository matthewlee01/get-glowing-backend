(ns globar.vendor-tests
  (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system! q]]
            [clojure.java.shell :refer [sh]]
            [clojure.data.json :as json]
            [globar.db :as db]))

(use-fixtures :once setup-test-system!)

; do some tests against the services for a vendor
(deftest test-vendor-query
  ;; get a a vendor and all the associated services
  (let [qstring (str "query{vendor_by_email(email: \"helen@gmail.com\")"
                              "{vendor_id user_id name_first "
                              "services {s_description s_duration s_name s_price s_type}  "
                              "services_summary {count min max}"
                              "}}")
        result (q qstring nil)]
    ;; confirm that there were 4 services found
    (is (= 4 (-> result
                 :data
                 :vendor_by_email
                 :services
                 count)))

    ;; confirm our understanding of the max and min price for these services
    (is (= 4 (-> result
                 :data
                 :vendor_by_email
                 :services_summary
                 :count)))
    (is (= 3000 (-> result
                    :data
                    :vendor_by_email
                    :services_summary
                    :min)))

    (is (= 10000 (-> result
                     :data
                     :vendor_by_email
                     :services_summary
                     :max)))))

(deftest test-vendor-list
  (let [qstring (str "query { vendor_list ( addr_city:\"Vancouver\"){vendor_id summary}}")]
    (is (= 3 (-> (q qstring nil)
                 :data
                 :vendor_list
                 count))))

  (let [qstring "query{vendor_list(addr_city:\"Vancouver\", service:\"nails\"){vendor_id summary}}"]
    (is (= 2 (-> (q qstring nil)
                 :data
                 :vendor_list
                 count)))))

(deftest test-vendor-creation
  ;; first create a new user
  (let [new-user {:name_first "I am the Test user"
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
                        "-d" (json/write-str new-user) "-X" 
                        "POST" "http://localhost:8889/user")
        post-clj (json/read-str (:out post-result) :key-fn keyword)

        ;; now create a new vendor
        new-vendor {:user-id (:user-id post-clj)
                    :summary "This is my story - I wish I had one, but this is all I got!"}

        ;; issue the curl call
        post-result (sh "curl" "-H" 
                        "Content-Type: application/json" 
                        "-d" (json/write-str new-vendor) "-X" 
                        "POST" "http://localhost:8889/vendor")
        post-clj (json/read-str (:out post-result) :key-fn keyword)]
    (is (= new-vendor (dissoc  post-clj 
                               :created-at
                               :radius 
                               :profile-pic
                               :updated-at
                               :vendor-id)))))

(deftest test-vendor-update
  ;; first create a new vendor
  (let [vendor (db/find-vendor-by-id 1300)
        updated (assoc vendor :summary "this is my new poopy summary")
        upd-ven (dissoc updated :updated-at :created-at)
        ;; issue the curl call
        post-result (sh "curl" "-H"
                        "Content-Type: application/json"
                        "-d" (json/write-str upd-ven) "-X"
                        "POST" "http://localhost:8889/vendor")
        post-clj (json/read-str (:out post-result) :key-fn keyword)]

    ;; confirm that what we wrote is what we get back from the reqeuest
    (is (= upd-ven (dissoc post-clj
                           :created-at
                           :updated-at)))))


                            
