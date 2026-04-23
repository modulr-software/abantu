(ns abantu.student-migrations.001-student-database-setup
  (:require [abantu.db.student]
            [abantu.db.master]
            [abantu.db.tables :as tables]
            [abantu.db.util :as db.util]))

(defn run-up! [context]
  (let [ds-student (:ds-student context)]
    (tables/create-tables!
     ds-student
     :abantu.db.student
     [:events])))

(defn run-down! [context]
  (let [ds-student (:ds-student context)
        id (:id context)]
    (tables/drop-all-tables! ds-student)
    (db.util/remove-db-files! :student {:id id})))

(comment

  ())
