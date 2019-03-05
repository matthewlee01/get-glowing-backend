(ns globar.core
  (:require [com.walmartlabs.lacinia :as lacinia]
            [clojure.java.browse :refer [browse-url]]
            [globar.system :as system]
            [clojure.walk :as walk]
            [com.stuartsierra.component :as component])
  (:gen-class))

;; this creates a new "system", where a system represents all of the components
;; required for the backend to function properly
(defonce system (system/new-system))


;; when this function generates a failed to parse graphQL query error,
;; try checking that the {} and () are matched in the nested query
(defn q
  "this function takes a string representing a graphql query,
  executes it, and prints the results to stdout"
  [query-string]
  (-> system
      :schema-provider
      :schema
      (lacinia/execute query-string nil nil)))


(defn start
  "this function provides a way to start up the system and
  open a browser tab"
  []
  (alter-var-root #'system component/start-system)
  (browse-url "http://localhost:8888/")
  :started)



(defn stop
  "this function stops the running system"
  []
  (alter-var-root #'system component/stop-system)
  :stopped)

(defn -main []
  (start))

