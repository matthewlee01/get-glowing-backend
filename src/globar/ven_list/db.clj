(ns globar.ven-list.db 
  (:require [globar.db :as db]
            [globar.schema :as schema]))

(defn get-ven-page
  [vendor-id page-size [sort-col sort-arg order] [filter-col filter-arg]]
  "takes a page request token and retrieves a page-size of values from the db"
  (let [sort-arg-val (case sort-col
                       "v.updated_at" (if sort-arg
                                        (java.sql.Timestamp/valueOf sort-arg))
                       sort-arg)
        query-str (str "SELECT v.updated_at, u.user_id, addr_city, name_first, name_last, vendor_id, v.profile_pic
                        FROM Vendors v
                        INNER JOIN Users u 
                        ON v.user_id = u.user_id "
                       (if (= (type filter-arg) java.lang.String)
                         (str "WHERE UPPER(" filter-col ") = UPPER(?) ")
                         (str "WHERE " filter-col " = ? "))
                       (if vendor-id
                         (str "AND ("
                               sort-col " > ?" 
                               " OR (" sort-col " = ? AND vendor_id > " vendor-id ")) "))
                       "ORDER BY " sort-col " " order ", vendor_id "
                       "LIMIT " page-size)]
    (->> (db/query (if vendor-id 
                     [query-str filter-arg sort-arg-val sort-arg-val]
                     [query-str filter-arg]))
        (map #(assoc % :services-summary (schema/services-summary nil nil %)
                       :rating-summary (schema/rating-summary nil nil %))) 
        (map #(update-in % [:updated-at] str)))))

