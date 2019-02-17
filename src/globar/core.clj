(ns globar.core
  (:require [com.walmartlabs.lacinia :as lacinia]
            [clojure.java.browse :refer [browse-url]]
            [globar.system :as system]
            [clojure.walk :as walk]
            [com.stuartsierra.component :as component]
            [globar.test_utils :as utils]))


(defonce system (system/new-system))


;; when this function generates a failed to parse graphQL query error,
;; try checking that the {} and () are matched in the nested query
(defn q [query-string]
  (-> system
      :schema-provider
      :schema
      (lacinia/execute query-string nil nil)
      utils/simplify))


(defn start []
  (alter-var-root #'system component/start-system)
  (browse-url "http://localhost:8888/")
  :started)



(defn stop []
  (alter-var-root #'system component/stop-system)
  :stopped)

