(ns abantu.migrate
  (:require [k16.mallard :as mallard]
            [k16.mallard.store.sqlite :as store]
            [k16.mallard.loader.fs :as loader.fs]
            [next.jdbc :as jdbc]
            [abantu.db.util :as db.util]
            [abantu.db.honey :as db]))

;; This is our interface for running migrations.
;;
;; Migrations will do the following:
;; - migrate or rollback updates on ALL affected dbs; when a migration is applied
;;   we want all of the databases and the generated schemas to stay in sync with eachother.
;; - seed the appropriate tables with data.
;; - (TODO) generate malli schemas to match the affected db schemas 

(def ^:private migrations
  (loader.fs/load! "src/abantu/migrations"))

(def ^:private student-migrations
  (loader.fs/load! "src/abantu/student_migrations"))

(defn run-student-migration! [id direction]
  (let [dir (name direction)
        context {:ds-student (db.util/conn :student id)
                 :id id}
        db-migrate (jdbc/get-datasource {:dbname (db.util/migration-db-path :student id)
                                         :dbtype "sqlite"})
        datastore (store/create-datastore
                   {:db db-migrate
                    :table-name "migrations"})]
    (mallard/run {:context context
                  :store datastore
                  :operations student-migrations}
                 (list dir))))

(defn run-student-migrations! [direction]
  (let [ds (db.util/conn :master)
        users (db/find ds {:tname :users
                           :ret :*})]
    (when (seq users)
      (run! #(run-student-migration! (:id %) direction) users))))

(defn create-student-db! [{:keys [id] :as user}]
  (try
    (run-student-migration! id :up)
    {:success true :error nil}
    (catch Exception _
      {:success false
       :error (ex-info (str  "Failed to create user database for id " id) {:user user})})))

(defn remove-student-db! [{:keys [id] :as user}]
  (try
    (run-student-migration! id :down)
    {:success true :error nil}
    (catch Exception _
      {:success false
       :error (ex-info (str "failed to delete user database for id " id) {:user user})})))

(defn run-migrations [args]
  (let [context {:db-master (db.util/conn :master)}
        db-migrate (jdbc/get-datasource {:dbname (db.util/db-path "migrate")
                                         :dbtype "sqlite"})
        datastore (store/create-datastore
                   {:db db-migrate
                    :table-name "migrations"})]
    (when (= (name (first args)) "down")
      (run-student-migrations! (first args)))
    (mallard/run {:context context
                  :store datastore
                  :operations migrations}
                 args)
    (when (= (name (first args)) "up")
      (run-student-migrations! (first args)))))

(defn -main [& args]
  (run-migrations args))

(comment
  (run-migrations '("up"))
  (run-migrations '("down"))

  (run-student-migration! 1 :up)

  (create-student-db! {:id 1})
  (remove-student-db! {:id 1})

  :end)
