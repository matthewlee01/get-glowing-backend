(ns globar.vendor-tests
  (:require [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system! q]]))

(use-fixtures :once setup-test-system!)

;; do some tests against the services for a vendor
(deftest test-vendor-query
  ;; get a a vendor and all the associated services
  (let [qstring (str "query{vendor_by_email(email: \"helen@gmail.com\")"
                              "{vendor_id name_first "
                              "services {s_description s_duration s_name s_price s_type}  "
                              "services_summary {count min max}"
                              "}}")
        result (q qstring nil)]
    (println "HELLO : " result)
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

(deftest test-vendor-creation
  (let [new-email "vendor@somedomain.net"
        new-password "myawesomepassword"
        qstring (str "mutation { create_vendor ( new_vendor: { email:\"" new-email "\"
                                                               password:\"" new-password "\"}){vendor_id email}}")]
    ;; when i try to create a bare vendor, does this call succeed?
    (is (= new-email (-> (q qstring nil)
                          :data
                          :create_vendor
                          :email)))))

(deftest test-vendor-list
  (let [qstring (str "query { vendor_list ( addr_city:\"Vancouver\"){vendor_id addr_city}}")]
    (is (= 3 (-> (q qstring nil)
                  :data
                  :vendor_list
                  count))))

  (let [qstring "query{vendor_list(addr_city:\"Vancouver\", service:\"nails\"){vendor_id addr_city}}"]
    (is (= 2 (-> (q qstring nil)
                  :data
                  :vendor_list
                  count)))))

(deftest test-vendor-update
  (let [email "mrbig123@somedomain.com"
        updateVendor {:upd_vendor {:vendor_id 1237
                                   :name_first "I am the Test user"
                                   :name_last "Andrrson-Coopers"
                                   :email email
                                   :addr_str_num "#2-5985"
                                   :addr_str_name "Canada Way SE"
                                   :addr_city "New Westminster"
                                   :addr_state "BC"
                                   :addr_postal "V5e-3N9"
                                   :phone "778-994-6836"
                                   :locale "Pacific Standard Time"
                                   :summary "This is my story - I wish I had one, but this is all I got!"}}]

    ;; when i try to update a vendor, does this succeed?
    (let [qstring (str "mutation ($upd_vendor:InputUpdateVendor!)
                    {update_vendor(upd_vendor: $upd_vendor) {vendor_id email}}")]
      (is (= (:locale updateVendor) (-> (q qstring updateVendor)
                                        :data
                                        :update_vendor
                                        :locale))))

    ;; when i read back the vendor i just updated, does it match the data i updated with?
    (let [qstring (str "query{vendor_by_email(email: \"" email "\"){vendor_id name_first name_last email
                        addr_str_num addr_str_name addr_city addr_state addr_postal phone locale summary}}")]
      (is (= (:upd_vendor updateVendor) (-> (q qstring nil)
                                            :data
                                            :vendor_by_email))))))

