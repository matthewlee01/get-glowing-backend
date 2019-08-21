(ns globar.ven-profile.db 
  (:require [globar.db :as db]))

(defn get-full-vendor-profile
  [vendor-id]
  "gets all of a vendor's personal info from the vendor and user tables and returns it to them. should be authenticated before using this"
  (-> (db/query ["SELECT name_first, name_last, email, phone,
                  addr_str_num, addr_str_name, addr_city, addr_state,
                  profile_pic, template, summary, addr_postal, locale
                  FROM Vendors v
                  INNER JOIN Users u
                  ON v.user_id = u.user_id
                  WHERE v.vendor_id = ?" vendor-id])
      first))
