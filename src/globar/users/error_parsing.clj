(ns globar.users.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  (condp ep/includes-str? spec-str
    :000))