(ns globar.load
  (:require [globar.db :as db]
            [io.pedestal.log :as log]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn load-edn
  "Load edn from an io/reader source (filename or io/resource)."
  [source]
  (try
    (with-open [r (io/reader source)]
      (edn/read (java.io.PushbackReader. r)))

    (catch java.io.IOException e
      (log/debug :load-edn (str "Couldn't open '%s': %s\n" source (.getMessage e))))

    (catch RuntimeException e
      (log/debug :load-edn (str "Error parsing edn file '%s': %s\n" source (.getMessage e))))))

(defn create-services [vendor-id service-info]
  (let [{:keys [name description type price duration]} service-info
        new-service {:vendor-id vendor-id
                     :s-name name
                     :s-description description
                     :s-type type
                     :s-price price
                     :s-duration duration}]
    (db/create-service new-service)))

(defn generate-UUID [] (.toString (java.util.UUID/randomUUID)))

(defn create-vendor [vendor-info]
  (let [{:keys [first-name last-name email city summary]} vendor-info
        new-user {:name-first first-name
                  :name-last last-name
                  :name (str first-name " " last-name)
                  :sub (generate-UUID)
                  :avatar "http://google.com/some/url/to/a/file.jpg"
                  :email email
                  :email_verified true
                  :addr-str-num "#2-5985"
                  :addr-str-name "Canada Way SE"
                  :addr-city city
                  :addr-state "BC"
                  :addr-postal "V5E-3N9"
                  :phone "778-994-6836"
                  :locale "Pacific Standard Time"}
        user (db/create-user new-user)]

    (if user
      (let [new-vendor {:user-id (:user-id user)
                        :summary summary
                        :profile-pic "placeholder.jpg"}
            vendor (db/create-vendor new-vendor)
            ven-id (:vendor-id vendor)
            services (:services vendor-info)]
        (println services)
        (map #(create-services ven-id %) services)))))

(defn load-vendors
  "this function loads a list of vendors from the filesystem"
  []
  (let [vendor-list (load-edn "resources/vendor-data.edn")]
    (println "read edn file, found it contains: " (count vendor-list) " entries.")
    (map create-vendor vendor-list)))

