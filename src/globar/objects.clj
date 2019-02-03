(ns globar.objects)


(def FIRST_AVAILABLE_ID 1000)

;; following Stuart Sierra's guidance to not have any global vars
;; instead adding a factory to instantiate within a function and pass
;; this value around between the functions
;; TODO: think about allowing for seeding this with some initial values
(defn objRepoFactory
  "this will instantiates a new object repository"
  []
  (atom {:currentID FIRST_AVAILABLE_ID}))

(defn addObject
  "adds a new object into the object repository"
  [objRepo obj]
  (let [addObjInternal (fn [db obj]
                         (let [currentID (:currentID db)]
                           (assoc db :currentID (+ currentID 1) currentID obj)))]

    (swap! objRepo addObjInternal obj)))

(defn getObject
  "retrieves an object from the object repository"
  [objRepo id]
  (get @objRepo id))

(defn removeObject
  "remove an object from the object repository"
  [objRepo id]
  (dissoc objRepo id))

;; this might have a different implementation given different
;; backing persistence stores
(defn objectExists?
  "check if an object specified by a given object ID exists"
  [objRepo id]
  (get @objRepo id))

(defn updateObject
  "update an existing object from the object repository
   if the object doesn't exist, do nothing"
  [objRepo id obj]
  (if (objectExists? objRepo id)
    (swap! objRepo #(assoc %1 %2 %3) id obj)
    (println "WARNING: trying to update an object that doesn't exist")))

