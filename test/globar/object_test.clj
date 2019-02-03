(ns globar.object-test
  (:require [clojure.test :refer :all]
            [globar.objects :refer :all]))

(def seed_obj {:name "test object" :field "another field"})
(def seed_obj2 {:name "jay" :age 49})

(deftest test-getObject
  (testing "Testing getObject with objects that don't exist"
    (is (= nil (let [repo (objRepoFactory)]
                 (getObject repo 0))))
    (is (= nil (let [repo (objRepoFactory)]
                 (getObject repo FIRST_AVAILABLE_ID)))))

  (testing "Testing getObject with objects that do exist"
    (is (= seed_obj (let [repo (objRepoFactory)]
                      (addObject repo seed_obj)
                      (getObject repo FIRST_AVAILABLE_ID))))
    (is (= seed_obj2 (let [repo (objRepoFactory)]
                       (addObject repo seed_obj)
                       (addObject repo seed_obj2)
                       (getObject repo (+ FIRST_AVAILABLE_ID 1)))))))

(deftest test-update
  (testing "Test updating the value of an object"
    (is (= seed_obj2 (let [repo (objRepoFactory)]
                       (addObject repo seed_obj)
                       (updateObject repo FIRST_AVAILABLE_ID seed_obj2)
                       (getObject repo FIRST_AVAILABLE_ID)))))

  (testing "Updating an object ID that doesn't exist should have no effect on the repo"
    (println "Expecting to see a warning printed in this test")
    (let [repo (objRepoFactory)
          orig @repo]
      (updateObject repo FIRST_AVAILABLE_ID seed_obj)
      (is (= orig @repo)))))
