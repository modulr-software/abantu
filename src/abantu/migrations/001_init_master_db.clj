(ns abantu.migrations.001-init-master-db
  (:require [abantu.admins :as admins]
            [abantu.db.master]
            [abantu.db.honey :as db]
            [abantu.db.tables :as tables]
            [abantu.datastore.config :as ds]
            [abantu.config :as conf]))

(defn run-up! [context]
  (let [ds-master (:db-master context)]

    ;; (tables/create-tables!
    ;;  ds-master
    ;;  :abantu.db.master
    ;;  [])

    ;; (db/insert! ds-master baselines-seed)
    ;; (db/insert! ds-master cadences-seed)
    ;; (db/insert! ds-master content-types-seed)
    ;; (db/insert! ds-master providers-seed)
    ;; (db/insert! ds-master categories-seed)
    ;; (db/insert! ds-master sectors-seed))
  ))

(defn run-down! [context]
  (let [ds-master (:db-master context)]
    ;;(tables/drop-all-tables! ds-master)
    ))

(comment
  (admins/read)
  ())
