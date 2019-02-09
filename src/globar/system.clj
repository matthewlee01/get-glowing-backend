(ns globar.system
  (:require [com.stuartsierra.component :as component]
            [globar.schema :as schema]
            [globar.server :as server]))

(defn new-system
  "this defines the component system map, that describes all the components
  that together compose the system"
  []
  (merge (component/system-map)
         (server/new-server)
         (schema/new-schema-provider)))
