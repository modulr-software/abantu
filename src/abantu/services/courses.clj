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
    (when (some? course)
     (assoc course :units units))))

(defn save-course! [ds course]
  )
(defn save-courses! [ds courses]
  )

(defn update-course [ds course]
  )

(defn delete-course [ds id]
  )


(comment
  (def ds (db/ds :master))
  (get-all ds)
  (get-course ds 1)
  ()
  )