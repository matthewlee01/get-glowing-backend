;; this module holds the code related to the http server that
;; uses pedestal to field http requests
(ns globar.server
  (:require [mount.core :refer [defstate]]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [io.pedestal.http :as http]
            [globar.schema :as schema]))

(defn start-server [port]
  (println "starting http server")
  (-> schema/schema-state
      (lp/service-map {:graphiql true
                       :port port})
      (assoc ::http/allowed-origins ["http://localhost:3449" "http://localhost:8888"])
      http/default-interceptors
      http/create-server
      http/start))

(defn stop-server [srv]
  (println "stopping http server")
  (http/stop srv))

;; this captures the state of the webserver
(defstate server-state :start (start-server 8888)
                       :stop (stop-server server-state))

