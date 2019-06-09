(ns globar.user-tests
   (:require [clojure.test :refer :all]
             [globar.test_utils :refer [q setup-test-system!]]
             [clojure.java.shell :refer [sh]]
             [clojure.data.json :as json]
             [clojure.set :refer [rename-keys]]
             [globar.db :as db]
             [fipp.edn :refer [pprint]]))

(use-fixtures :once setup-test-system!)

(deftest test-user-creation
  ;; first create a new user
  (let [new-user {:given_name     "I am the Test user"
                  :family_name    "Andrrson-Coopers"
                  :name           "Billy the Big Bad Bob"
                  :sub            "social google | some funny; characters@?"
                  :avatar         "http://google.com/some/url/to/a/file.jpg"
                  :email          "test@test.com"
                  :email_verified true
                  :locale         "Pacific Standard Time"}

        ;; issue the curl call
        post-result (sh "curl" "-H"
                        "Content-Type: application/json"
                        "-d" (json/write-str new-user) "-X"
                        "POST" "http://localhost:8889/user")
        post-clj (json/read-str (:out post-result) :key-fn keyword)]

    ;; confirm that what we wrote is what we get back from the reqeuest
    (is (= (:given_name new-user) (:name-first post-clj)))
    (is (= (:family_name new-user) (:name-last post-clj)))
    (is (= (:name new-user) (:name post-clj)))
    (is (= (:sub new-user) (:sub post-clj)))
    (is (= (:avatar new-user) (:avatar post-clj)))
    (is (= (:email new-user) (:email post-clj)))
    (is (= (:email_verified new-user) (:email-verified post-clj)))
    (is (= (:locale new-user) (:locale post-clj)))))

(deftest test-user-update
  ;; first create a new user
  (let [user (db/find-user-by-id 37)
        updated  (assoc user :sub "hello mr poopy")
        upd-user (dissoc updated :updated-at :created-at)
        ;; issue the curl call
        post-result (sh "curl" "-H"
                        "Content-Type: application/json"
                        "-d" (json/write-str upd-user) "-X"
                        "POST" "http://localhost:8889/user")
        post-clj (json/read-str (:out post-result) :key-fn keyword)]

    ;; confirm that what we wrote is what we get back from the reqeuest
    (is (= upd-user (dissoc post-clj
                            :created-at
                            :updated-at)))))
                            
                            
