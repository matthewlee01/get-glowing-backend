(ns globar.test_utils
  (:require [clojure.walk :as walk]
            [mount.core :as mount]
            [globar.server :as server :refer [server-state]]
            [globar.schema :as schema]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.java.shell :refer [sh]])
  (:import (clojure.lang IPersistentMap)))


(defn simplify
  "Converts all ordered maps nested within the map into standard hash maps, and
   sequences into vectors, which makes for easier constants in the tests, and eliminates ordering problems."
  [m]
  (walk/postwalk
    (fn [node]
      (cond
        (instance? IPersistentMap node)
        (into {} node)

        (seq? node)
        (vec node)

        :else
        node))
    m))

(defn start-test-system!
  "Creates a new system suitable for testing, and ensures
  that the HTTP port won't conflict with a default running system"
  []
  ;; reset the db
  (sh "./bin/reset-db.sh")
  (mount/start-with-states {#'globar.server/server-state {:start #(server/start-server 8889)
                                                          :stop #(server/stop-server server-state)}}))
(defn stop-test-system!
  "Stops the system after tests have completed"
  []
  (mount/stop))

(defn q
  "Extracts the compiled schema and executes a query"
  [query variables]
  (-> schema/schema-state
      (lacinia/execute query variables nil)
      simplify))


(defn setup-test-system! [f]
  (start-test-system!)
  (try
    (f)
    (catch Exception e (str "caught exception: " (.getMessage e)))
    (finally (stop-test-system!)))
  (stop-test-system!))
