(ns globar.users.core
  (:require [globar.db :as db]
            [clojure.spec.alpha :as s]
            [globar.error-parsing :as ep]
            [globar.users.db :as u-db]
            [globar.users.error-parsing :as u-ep]
            [io.pedestal.http :as http]))

(def EMAIL-REGEX #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$") ; some characters + @ + some characters + 2-63 letters

(s/def ::email
  (s/nilable (s/and string?
                    #(re-matches EMAIL-REGEX %))))

(s/def ::name-first
  (s/nilable string?))

(s/def ::name-last
  (s/nilable string?))

(s/def ::name
  (s/nilable string?))

(s/def ::email-verified
  (s/nilable (s/or :true true?
                   :false false?)))

(s/def ::is-vendor
  (s/or :true true?
        :false false?))

(s/def ::avatar
  (s/nilable string?))

(s/def ::locale
  (s/nilable string?))

(s/def ::sub
  string?)

(s/def ::user-id
  (s/and integer?
         db/find-user-by-id))

(s/def ::valid-user
  (s/keys :opt-un [::name-first ::name-last ::name ::email ::email-verified ::is-vendor ::avatar ::user-id ::locale ::sub]))

(defn upsert-user
  [request]
  (let [user (get-in request [:json-params])]
    ;(println "lmao")
    ;(println request)
    (if (s/valid? ::valid-user user)
      (if (:user-id user)
          (do (u-db/update-user user)
              (http/json-response (db/find-user-by-id (:user-id user))))
          (http/json-response (u-db/create-user user)))
      (http/json-response {:error (->> user
                                       (s/explain-str ::valid-user)
                                       (ep/get-error-data ep/ERROR_MSG_SET_EN u-ep/get-error-code))}))))