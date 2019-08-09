(ns globar.ven-list-tests
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [globar.test_utils :refer [setup-test-system!]]
            [globar.error-parsing :as ep]
            [globar.ven-list.error-parsing :as vl-ep]
            [globar.ven-list.core :as vl-c]))

(use-fixtures :each setup-test-system!)

(deftest ven-list-spec
  (let [good-token {:sort-by ["name_first" "Wishful" "asc"]
                    :filter-by [["city" "vancouver"]]
                    :vendor-id 1234
                    :page-size 2}
        incomplete-token (dissoc good-token :sort-by)
        bad-id-token (assoc good-token :vendor-id 9856)
        neg-size-token (assoc good-token :page-size -4)
        invalid-sort-token (assoc good-token :sort-by ["name_first" "Wishful" "wooohoooo"])
        malformed-token ["hey" ["what's up" 2345 {}]]
        get-error-code #(vl-ep/get-error-code (s/explain-str ::vl-c/valid-token %))]
    (is (= (get-error-code incomplete-token) :701))
    (is (= (get-error-code bad-id-token) :105))
    (is (= (get-error-code neg-size-token) :704))
    (is (= (get-error-code invalid-sort-token) :713))
    (is (= (get-error-code malformed-token) :100))))
        
    

