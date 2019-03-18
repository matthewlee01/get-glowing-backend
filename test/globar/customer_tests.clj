(ns globar.customer-tests
   (:require [clojure.test :refer :all]
            [globar.test_utils :refer [start-test-system! stop-test-system! q]]))


(deftest test-customer-creation
  (start-test-system!)
  (let [new-email "billybob@somedomain.net"
        qstring (str "mutation { create_customer ( new_cust: { email:\"" new-email "\"}){cust_id email}}")]
    (try

      ;; when i try to create a bare customer, does this call succeed?
      (is (= new-email (-> (q qstring nil)
                           :data
                           :create_customer
                           :email)))
      (catch Exception e)
      (finally (stop-test-system!)))))

(deftest test-customer-update
  (start-test-system!)
  (let [email "mrbig123@somedomain.com"
        updateUser {:upd_cust {:cust_id 37
                               :name_first "I am the Test user"
                               :name_last "Andrrson-Coopers"
                               :email email
                               :addr_str_num "#2-5985"
                               :addr_str_name "Canada Way SE"
                               :addr_city "New Westminster"
                               :addr_state "BC"
                               :addr_postal "V5e-3N9"
                               :phone "778-994-6836"
                               :locale "Pacific Standard Time"}}]
    (try

       ;; when i try to update a customer, does this succeed?
       (let [qstring (str "mutation ($upd_cust:InputUpdateCustomer!)
                      {update_customer(upd_cust: $upd_cust) {cust_id email}}")]
         (is (= (:locale updateUser) (-> (q qstring updateUser)
                                         :data
                                         :update_customer
                                         :locale))))

       ;; when i read back the customer i just updated, does it match the data i updated with?
       (let [qstring (str "query{customer_by_email(email: \"" email "\"){cust_id name_first name_last
                           email addr_str_num addr_str_name addr_city addr_state addr_postal phone locale}}")]
         (is (= (:upd_cust updateUser) (-> (q qstring nil)
                                           :data
                                           :customer_by_email))))

       (catch Exception e)
       (finally (stop-test-system!)))))


