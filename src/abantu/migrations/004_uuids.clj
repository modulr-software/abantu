(ns abantu.migrations.004-uuids
  (:require [abantu.db.master]
            [abantu.db.honey :as hon]
            [honey.sql.helpers :as hsql]))

(defn run-up! [context]
  (let [ds-master (:db-master context)]
    (hon/execute!
     ds-master
     (hsql/alter-table :courses (hsql/add-column :uuid :text :not nil))
     {})
    (hon/execute!
     ds-master
     (hsql/alter-table :units (hsql/add-column :uuid :text :not nil))
     {})
    (hon/execute!
     ds-master
     (hsql/alter-table :exercises (hsql/add-column :uuid :text :not nil))
     {})))

(defn run-down! [context]
  (let [ds-master (:db-master context)]
    (hon/execute!
     ds-master
     (hsql/alter-table :courses (hsql/drop-column :if-exists :uuid))
     {})
    (hon/execute!
     ds-master
     (hsql/alter-table :units (hsql/drop-column :if-exists :uuid))
     {})
    (hon/execute!
     ds-master
     (hsql/alter-table :exercises (hsql/drop-column :if-exists :uuid))
     {})))
