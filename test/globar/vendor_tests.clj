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

