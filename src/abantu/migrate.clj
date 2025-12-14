(ns abantu.migrate
  (:require [k16.mallard :as mallard]
            [k16.mallard.store.sqlite :as store]
            [k16.mallard.loader.fs :as loader.fs]
            [next.jdbc :as jdbc]
            [abantu.db.util :as db.util]
            [abantu.db.honey :as db]
            [abantu.db.tables :as tables]))

;; This is our interface for running migrations.
;;
;; Migrations will do the following:
;; - migrate or rollback updates on ALL affected dbs; when a migration is applied
;;   we want all of the databases and the generated schemas to stay in sync with eachother.
;; - seed the appropriate tables with data.
;; - (TODO) generate malli schemas to match the affected db schemas 

(def ^:private migrations
  (loader.fs/load! "src/source/migrations"))

(def ^:private bundle-migrations
  (loader.fs/load! "src/source/bundle_migrations"))

(defn run-migrations [args]
  (let [context {:db-master (jdbc/get-datasource {:dbname (db.util/db-path "master") :dbtype "sqlite"})}
        db-migrate (jdbc/get-datasource {:dbname (db.util/db-path "migrate") :dbtype "sqlite"})
        datastore (store/create-datastore
                   {:db db-migrate
                    :table-name "migrations"})]
    (mallard/run {:context context
                  :store datastore
                  :operations migrations}
                 args)))

(defn migrate-bundle [bundle-id args]
  (let [db-name (db.util/db-name :bundle bundle-id)
        context {:db-bundle (jdbc/get-datasource {:dbname (db.util/db-path db-name)
                                                  :dbtype "sqlite"})}
        datastore (store/create-datastore
                   {:db (:db-bundle context)
                    :table-name "migrations"})]
    (mallard/run {:context context
                  :store datastore
                  :operations bundle-migrations}
                 args)))

(defn run-bundle-migrations [args]
  (let [ds-master (db.util/conn :master)
        bundles (if (some #(= % "bundles") (tables/table-names ds-master))
                  (db/find ds-master {:tname :bundles
                                      :ret :*})
                  [])]
    (run! #(migrate-bundle (:id %) args) bundles)))

(defn -main [& args]
  (run-bundle-migrations args)
  (run-migrations args))
