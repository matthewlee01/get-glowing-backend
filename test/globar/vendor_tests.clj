(ns globar.vendor-tests
  (:require [clojure.test :refer :all]
            [globar.system :as system]
            [globar.test_utils :refer [simplify]]
            [com.stuartsierra.component :as component]
            [com.walmartlabs.lacinia :as lacinia]))

(defn ^:private create-test-system
  "Creates a new system suitable for testing, and ensures
  that the HTTP port won't conflict with a default running system"
  []
  (-> (system/new-system)
      (assoc-in [:server :port] 8989)))

(defn ^:private q
  "Extracts the compiled schema and executes a query"
  [system query variables]
  (-> system
      (get-in [:schema-provider :schema])
      (lacinia/execute query variables nil)
      simplify))

(deftest test-vendor-creation
  (let [system (component/start-system (create-test-system))
        new-email "billybob@somedomain.net"
        qstring (str "mutation { create_vendor ( new_vendor: { email:\"" new-email "\"}){vendor_id email}}")]
    (try

      ;; when i try to create a bare vendor, does this call succeed?
      (let [qresult (q system qstring nil)]
        (is (= new-email (:email (:create_vendor (:data qresult))))))
      (catch Exception e)
      (finally (component/stop-system system)))))

(deftest test-vendor-update
  (let [system (component/start-system (create-test-system))
        email "mrbig123@somedomain.com"
        updateVendor {:upd_cust {:vendor_id 1237
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
    (try

      ;; when i try to update a vendor, does this succeed?
      (let [qstring (str "mutation ($upd_vendor:InputUpdateVendor!)
                      {update_vendor(upd_vendor: $upd_vendor) {vendor_id email}}")
            qresult (q system qstring updateVendor)]
        (is (= (:locale updateVendor) (get-in qresult [:data :update_vendor :locale]))))

      ;; when i read back the vendor i just updated, does it match the data i updated with?
      (let [qstring (str "query{vendor_by_email(email: \"" email "\"){vendor_id name_first name_last email
                          addr_str_num addr_str_name addr_city addr_state addr_postal phone locale summary}}")
            qresult (q system qstring nil)]
        (println qstring)
        (is (= (:upd_vendor updateVendor) (get-in qresult [:data :vendor_by_email]))))

      (catch Exception e)
      (finally (component/stop-system system)))))


