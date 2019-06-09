(ns globar.core
  (:require [com.walmartlabs.lacinia :as lacinia]
            [clojure.java.browse :refer [browse-url]]
            [clojure.walk :as walk]
            [globar.schema :refer [schema-state]]
            [mount.core :as mount]
            [globar.server :as server]
            [globar.load :as load])
  (:gen-class))


;; when this function generates a failed to parse graphQL query error,
;; try checking that the {} and () are matched in the nested query
(defn q
  "this function takes a string representing a graphql query,
  executes it, and prints the results to stdout"
  [query-string]
  (-> schema-state
      (lacinia/execute query-string nil nil)))


(defn start
  "this function provides a way to start up the system and
  open a browser tab"
  []
  (mount/start)
  (browse-url "http://localhost:8888/")
  :started)

(defn stop
  "this function stops the running system"
  []
  (mount/stop)
  :stopped)

(defn load-vendors []
  (load/load-vendors))

(defn -main []
  (start))

