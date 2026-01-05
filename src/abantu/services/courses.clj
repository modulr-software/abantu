(ns abantu.services.courses
  (:require [abantu.db.interface :as db]
            [abantu.services.units :as units]))

(defn get-all [ds]
  (db/find ds {:tname :courses
               :ret :*}))

(defn get-course [ds id]
  (let [course (db/find ds {:tname :courses
                            :where [:= :id id]
                            :ret :1})
        units (units/get-units ds id)]
    (assoc course :units units)))