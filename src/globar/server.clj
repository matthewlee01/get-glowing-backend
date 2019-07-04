;; this module holds the code related to the http server that
;; uses pedestal to field http requests
(ns globar.server
  (:require [mount.core :refer [defstate]]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [io.pedestal.http :as http]
            [globar.schema :refer [schema-state]]
            [globar.config :as config]
            [globar.rest-api :as rest-api]
            [io.pedestal.http.route :as route]))


(defn start-server [port]
  (println "starting http server with schema" schema-state)
  (-> schema-state
      (lp/service-map {:graphiql true
                       :port port})
      (assoc ::http/allowed-origins (config/CORS-whitelist))
      (assoc ::http/file-path "resources/public")
      (update-in [::http/routes] #(concat (route/expand-routes %) rest-api/rest-api-routes))
      http/default-interceptors
      http/create-server
      http/start))

(defn stop-server [srv]
  (println "stopping http server")
  (http/stop srv))

;; this captures the state of the webserver
(defstate server-state :start (start-server 8888)
                       :stop (stop-server server-state))

