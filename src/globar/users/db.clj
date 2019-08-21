(ns globar.users.db
  (:require [globar.db :as db]
            [clojure.java.jdbc :as jdbc]
            [io.pedestal.log :as log]))

(defn create-user
  "Adds a new user object, or changes the values of an existing rating if one exists"
  [user-info]
  (log/debug ::create-user user-info)
  (let [field-spec (select-keys user-info [:name-first :name-last :name :email :email-verified
                                           :is-vendor :addr-str-num :addr-str-name :addr-city
                                           :addr-state :addr-postal :phone :locale :avatar :sub])
        db-field-spec (clojure.set/rename-keys field-spec
                                               {:name-first :name_first
                                                :name-last :name_last
                                                :email-verified :email_verified
                                                :is-vendor :is_vendor
                                                :addr-str-num :addr_str_num
                                                :addr-str-name :addr_str_name
                                                :addr-city :addr_city
                                                :addr-state :addr_state
                                                :addr-postal :addr_postal})
        result (jdbc/insert! db/db-conn :Users 
                             db-field-spec
                             {:identifiers #(.replace % \_\-)})]
    (first result)))

(defn update-user
  "Adds a new user object, or changes the values of an existing rating if one exists"
  [new-user]
  (log/debug ::update-user new-user)
  (let [field-spec (select-keys new-user [:name-first :name-last :name :email :email-verified
                                          :is-vendor :addr-str-num :addr-str-name :addr-city
                                          :addr-state :addr-postal :phone :locale :avatar :sub])
        updated-field-spec (-> (db/find-user-by-id (:user-id new-user))
                               (merge field-spec))
        db-field-spec (clojure.set/rename-keys field-spec
                                            {:name-first :name_first
                                             :name-last :name_last
                                             :email-verified :email_verified
                                             :is-vendor :is_vendor
                                             :addr-str-num :addr_str_num
                                             :addr-str-name :addr_str_name
                                             :addr-city :addr_city
                                             :addr-state :addr_state
                                             :addr-postal :addr_postal})]

    (jdbc/update! db/db-conn :Users db-field-spec ["user_id = ?" (:user-id new-user)])
    updated-field-spec))
