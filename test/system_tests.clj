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

(deftest can-read-users+vendors
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
                       "{user_by_id(id:1410) {name password user_id}}"
                       nil)]
        (is (= {:data {:user_by_id {:name "bleedingedge"
                                    :password "password2"
                                    :user_id 1410}}}
               results)))

      (let [results (q system
                       "{user_by_id(id: 37) {name user_id ratings
                                                             {vendor {name} rating}}}"
                       nil)]
        (is (= {:data {:user_by_id {:name "curiousattemptbunny"
                                    :user_id 37
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
                       "{user_by_id(id: 37) {name user_id ratings
                                                            {vendor {name} rating}}}"
                       nil)]
        (is (= results
               {:data {:user_by_id {:name "curiousattemptbunny"
                                         :user_id 37
                                         :ratings [ {:vendor {:name "Toes for Joes"}
                                                     :rating 3}
                                                   {:vendor {:name "War Paint"}
                                                    :rating 5}]}}})))

      (q system
         "mutation {rate_vendor(user_id: 37, vendor_id: 1236, rating: 2) {rating_summary{count average}}}"
         nil)

      (let [results (q system
                       "{user_by_id(id: 37) {name user_id ratings
                                                            {vendor {name} rating}}}"
                       nil)]
        (is (= results
               {:data {:user_by_id {:name "curiousattemptbunny"
                                    :user_id 37
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

