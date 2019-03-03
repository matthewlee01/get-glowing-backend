(ns globar.system
  (:require [com.stuartsierra.component :as component]
            [globar.schema :as schema]
            [globar.server :as server]
            [globar.db :as db]))

;; this is the system definition.  all new components should be added to this
(defn new-system
  "this defines the component system map, that describes all the components
  that together compose the system"
  []
  (merge (component/system-map)
         (server/new-server)
         (schema/new-schema-provider)
         (db/new-db)))
