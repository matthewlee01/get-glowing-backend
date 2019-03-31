;; this module holds the code related to the http server that
;; uses pedestal to field http requests
(ns globar.server
  (:require [mount.core :refer [defstate]]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [io.pedestal.http :as http]
            [globar.schema :as schema]
            [globar.config :as config]))

(defn CORS-whitelist []
  (if config/debug?
    ["http://localhost:3449" "http://localhost:8888"] ;; allow requests from figwheel and the graphiql app
    ["http://archon.j3mc.ca"]))                     ;; in production we only allow access from our webapp

(defn start-server [port]
  (println "starting http server with schema" schema/schema-state)
  (-> schema/schema-state
      (lp/service-map {:graphiql true
                       :port port})
      (assoc ::http/allowed-origins (CORS-whitelist))
      (assoc ::http/file-path "resources/public")
      http/default-interceptors
      http/create-server
      http/start))

(defn stop-server [srv]
  (println "stopping http server")
  (http/stop srv))

;; this captures the state of the webserver
(defstate server-state :start (start-server 8888)
                       :stop (stop-server server-state))

