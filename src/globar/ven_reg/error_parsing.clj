(ns globar.ven-reg.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  "takes a spec error data string and converts it to an error code."
  (condp ep/includes-str? spec-str
    ;; :000: unknown error, ideally this should never happen
    :000))


