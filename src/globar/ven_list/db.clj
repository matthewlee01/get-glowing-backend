(ns globar.ven-list.db 
  (:require [globar.db :as db]
            [globar.schema :as schema]))

(defn city-filter-clause
  [city]
  (str "UPPER(addr_city) = UPPER('" city "')"))

(defn max-cost-filter-clause
  [max-cost]
  (str "(SELECT MIN(s.s_price)
         FROM Services s
         WHERE s.vendor_id = v.vendor_id) <= CAST(" max-cost " AS int)"))

(defn min-cost-filter-clause
  [min-cost]
  (str "(SELECT MAX(s.s_price)
         FROM Services s
         WHERE s.vendor_id = v.vendor_id) >= CAST(" min-cost " AS int)"))

(defn min-rating-filter-clause
  [min-rating]
  (str "(SELECT AVG(r.rating)
         FROM vendor_rating r
         WHERE r.vendor_id = v.vendor_id) >= CAST(" min-rating " AS decimal)"))

(defn generate-filter-clause
  [[filter-col filter-arg]]
  "generates a filter clause to inject into the paging query"
  (case filter-col
    "city" (city-filter-clause filter-arg)
    "max-cost" (max-cost-filter-clause filter-arg)
    "min-cost" (min-cost-filter-clause filter-arg)
    "min-rating" (min-rating-filter-clause filter-arg)))

(defn generate-filter-clauses
  [filters]
  (->> (map generate-filter-clause filters)
       (reduce #(str %1 " AND " %2))))

(defn generate-paging-clause
  [sort-col vendor-id order]
  "returns the clause that calculates which items should be in the next page. if vendor-id is nil, then this is the first page and this clause shouldn't be used"
  (when vendor-id
    (str " AND ("
         sort-col (if (= order "asc") " >" " <") " ? "
         "OR (" sort-col " = ? AND vendor_id > " vendor-id ")) ")))
  
(defn get-ven-page
  [vendor-id page-size [sort-col sort-arg order] filters]
  "takes a page request token and retrieves a page-size of values from the db"
  (let [sort-arg-val (case sort-col
                       "v.updated_at" (if sort-arg
                                        (java.sql.Timestamp/valueOf sort-arg))
                       sort-arg)
        query-str (str "SELECT v.updated_at, u.user_id, addr_city, name_first, name_last, vendor_id, v.profile_pic
                        FROM Vendors v
                        INNER JOIN Users u 
                        ON v.user_id = u.user_id 
                        WHERE "
                       (generate-filter-clauses filters)
                       (generate-paging-clause sort-col vendor-id order)
                       "ORDER BY " sort-col " " order ", vendor_id "
                       "LIMIT " page-size)]
    (->> (db/query (if vendor-id 
                     [query-str sort-arg-val sort-arg-val]
                     [query-str]))
        (map #(assoc % :services-summary (schema/services-summary nil nil %)
                       :rating-summary (schema/rating-summary nil nil %))) 
        (map #(update-in % [:updated-at] str)))))


