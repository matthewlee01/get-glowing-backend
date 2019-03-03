(ns system_tests
  (:require [clojure.test :refer :all]
            [globar.system :as system]
            [globar.test_utils :refer [simplify]]
            [com.stuartsierra.component :as component]
            [com.walmartlabs.lacinia :as lacinia]))

(defn ^:private test-system
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

(deftest can-read-customers+vendors
  (let [system (component/start-system (test-system))]
    (try
      (let [results (q system
                       "{vendor_by_id(id:1234) {name summary vendor_id}}"
                       nil)]
        (is (= {:data {:vendor_by_id {:name "Toes for Joes"
                                      :summary "We make sure your toes don't look Dumb and Dumber"
                                      :vendor_id 1234}}}
               results)))

      (let [results (q system
                       "{customer_by_id(id:1410) {name password cust_id}}"
                       nil)]
        (is (= {:data {:customer_by_id {:name "bleedingedge"
                                        :password "password2"
                                        :cust_id 1410}}}
               results)))

      (let [results (q system
                       "{customer_by_id(id: 37) {name cust_id ratings
                                                             {vendor {name} rating}}}"
                       nil)]
        (is (= {:data {:customer_by_id {:name "curiousattemptbunny"
                                        :cust_id 37
                                        :ratings [ {:vendor {:name "Toes for Joes"}
                                                    :rating 3}
                                                   {:vendor {:name "War Paint"}
                                                    :rating 5}]}}}

               results)))

      (catch Exception e)
      (finally (component/stop-system system)))))

(deftest can-write-reviews
  (let [system (component/start-system (test-system))]
    (try
      (let [results (q system
                       "{cust_by_id(id: 37) {name cust_id ratings
                                                            {vendor {name} rating}}}"
                       nil)]
        (is (= results
               {:data {:cust_by_id {:name "curiousattemptbunny"
                                         :cust_id 37
                                         :ratings [ {:vendor {:name "Toes for Joes"}
                                                     :rating 3}
                                                   {:vendor {:name "War Paint"}
                                                    :rating 5}]}}})))

      ;; create a new review for Vera's Butt Wax and check that the results are what we're looking for
      (is (= (q system
                "mutation {rate_vendor(cust_id: 37, vendor_id: 1236, rating: 2) {name rating_summary{count average}}}"
                nil)
             {:data {:rate_vendor {:name "Vera's Butt Wax"
                                   :rating_summary {:count 2
                                                    :average 3.0}}}}))

      (let [results (q system
                       "{customer_by_id(id: 37) {name cust_id ratings
                                                            {vendor {name} rating}}}"
                       nil)]
        (is (= results
               {:data {:customer_by_id {:name "curiousattemptbunny"
                                        :cust_id 37
                                        :ratings [ {:vendor {:name "Toes for Joes"}
                                                    :rating 3}
                                                  {:vendor {:name "War Paint"}
                                                   :rating 5}
                                                  {:vendor {:name "Vera's Butt Wax"}
                                                   :rating 2}]}}})))
      (catch Exception e)
      (finally (component/stop-system system)
               ;; since we added some data, we need to re-init the db for subsequent tests
               (clojure.java.shell/sh "./bin/setup-db.sh")))))


