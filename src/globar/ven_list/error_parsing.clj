(ns globar.ven-list.error-parsing
  (:require [globar.error-parsing :as ep]))

(defn get-error-code
  [spec-str]
  (condp ep/includes-str? spec-str
    "failed: (contains? % :page-size)" :700
    "failed: (contains? % :sort-by)" :701
    "failed: (contains? % :filter-by)" :702
    "failed: (contains? % :vendor-id)" :703
    "failed: pos? in: [:page-size]" :704
    "failed: (= (count %) 2) in: [:filter-by" :705
    "failed: (= (count %) 3) in: [:sort-by" :706
    "failed: (string? (first %)) in: [:filter-by" :707
    "failed: (string? (first %)) in: [:sort-by" :708
    "failed: integer? in: [:page-size]" :709
    "failed: vector? in: [:sort-by]" :710
    "failed: vector? in: [:filter-by]" :711
    "failed: vector? in: [:filter-by " :712 ;distinction between [:filter-by] and [:filter-by i]
    "failed: (= (last %) " :713
    (ep/common-get-error-code spec-str)))

(def ERROR_CODE_KEY
  {:700 :invalid-data-msg
   :701 :invalid-data-msg
   :702 :invalid-data-msg
   :703 :invalid-data-msg
   :704 :invalid-data-msg
   :705 :invalid-data-msg
   :706 :invalid-data-msg
   :707 :invalid-data-msg
   :708 :invalid-data-msg
   :709 :invalid-data-msg
   :710 :invalid-data-msg
   :711 :invalid-data-msg
   :712 :invalid-data-msg
   :713 :invalid-data-msg})
