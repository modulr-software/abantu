(ns abantu.db-test-utils
  (:require [abantu.db.util :as db.util]
            [next.jdbc :as jdbc]
            [k16.mallard :as mallard]
            [k16.mallard.store.sqlite :as store]
            [k16.mallard.loader.fs :as loader.fs]))

(defn create-test-db!
  "Creates a fresh test_<id> sqlite db and runs all master migrations
  against it. Returns an open connection to the migrated db."
  [id]
  (db.util/remove-db-files! :test id)
  (let [ds (db.util/conn :test id)
        db-migrate (jdbc/get-datasource
                    {:dbname (db.util/migration-db-path :test id)
                     :dbtype "sqlite"})
        datastore (store/create-datastore
                   {:db db-migrate
                    :table-name "migrations"})]
    (mallard/run {:context {:db-master ds}
                  :store datastore
                  :operations (loader.fs/load! "src/abantu/migrations")}
                 ["up"])
    ds))

(defn create-test-student-db!
  "Creates a fresh test_<id> sqlite db and runs all student migrations
  against it. `master-ds` is the (test) master connection the student
  migrations copy per-creator data from. Returns an open connection to
  the migrated db."
  [id master-ds]
  (db.util/remove-db-files! :test id)
  (let [ds-student (db.util/conn :test id)
        db-migrate (jdbc/get-datasource
                    {:dbname (db.util/migration-db-path :test id)
                     :dbtype "sqlite"})
        datastore (store/create-datastore
                   {:db db-migrate
                    :table-name "migrations"})]
    (with-redefs [db.util/conn (fn [& _] master-ds)]
      (mallard/run {:context {:ds-student ds-student
                              :id id}
                    :store datastore
                    :operations (loader.fs/load! "src/abantu/student_migrations")}
                   ["up"]))
    ds-student))

(defn cleanup-test-db! [id]
  (try (db.util/remove-db-files! :test id) (catch Throwable _))
  nil)

(comment
  (require '[source-be.db-test-utils :as db-test-utils]
           '[abantu.db.honey :as hon])

  (def master-ds (db-test-utils/create-test-db! 1))
  (def student-ds (db-test-utils/create-test-student-db! 2 master-ds))

  ;; verify schema landed
  (hon/tables master-ds)
  (hon/tables student-ds)

  (db-test-utils/cleanup-test-db! 1)
  (db-test-utils/cleanup-test-db! 2)

  :end)
