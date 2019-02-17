;; this module holds the code related to the http server that
;; uses pedestal to field http requests
(ns globar.server
  (:require [com.stuartsierra.component :as component]
            [com.walmartlabs.lacinia.pedestal :as lp]
            [io.pedestal.http :as http]))

;; this record implements the Lifecycle protocol to implement
;; the start and stop behaviour for the http server
(defrecord Server [schema-provider server port]

  component/Lifecycle

  (start [this]
    (assoc this :server (-> schema-provider
                            :schema
                            (lp/service-map {:graphiql true
                                             :port port})
                            http/create-server
                            http/start)))

  (stop [this]
    (http/stop server)
    (assoc this :server nil)))


;; this instantiates a new Server record and establishes
;; the dependency on :schema-provider
(defn new-server []
  {:server (component/using (map->Server {:port 8888})
                            [:schema-provider])})
