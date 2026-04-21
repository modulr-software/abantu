(ns abantu.db.util
  (:require [abantu.config :as conf]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [clojure.java.io :as io]))


(defn db-path [dbname]
  (let [db-dir (conf/read-value :database :dir)]
    (str
     db-dir
     (when (not (= (last db-dir) \/))
       "/")
     dbname)))

(defn db-name
  ([type]
   (name type))
  ([type id]
   (str (name type) "_" id)))

(defn- -conn [dbname]
  (prn "db-name" (db-path dbname))
  (let [conn (-> {:dbtype (conf/read-value :database :type)}
                 (merge {:dbname (db-path dbname)})
                 (jdbc/get-connection))]
    (jdbc/execute! conn ["PRAGMA journal_mode = WAL;"])
    (jdbc/execute! conn ["PRAGMA synchronous = NORMAL;"])
    (jdbc/with-options conn {:builder-fn rs/as-unqualified-lower-maps})
    conn))

(defn- validate-db-type [db-type]
  (or (= db-type :master) (= db-type :student) (= db-type :creator)))


(defn conn
  ([]
   (conn :master))
  ([db-type]
   (assert (= db-type :master))
   (-conn (db-name db-type)))
  ([db-type id]
   (assert (validate-db-type db-type))
   (-conn (db-name db-type id))))

(defn migration-db-path
  ([] (db-path "migrate"))
  ([dbtype id]
   (db-path
    (str (name dbtype) "_" id "_" "migrate"))))

(defn all-db-paths
  ([]
   [(db-path "master")
    (str (db-path "master") "-shm")
    (str (db-path "master") "-wal")
    (migration-db-path)])
  ([db-type id]
   (let [full-path (-> (db-name db-type id)
                       (db-path))]
     [full-path
      (str full-path "-shm")
      (str full-path "-wal")
      (migration-db-path db-type id)])))

(defn- remove-db-file! [path]
  (when (.exists (io/file path))
    (io/delete-file path)))
(defn remove-db-files!
  ([]
   (run! #(clojure.java.io/delete-file %) (all-db-paths)))
  ([dbtype id]
   (run! remove-db-file! (all-db-paths dbtype id))))

(comment
  (all-db-paths)
  (all-db-paths :student 1)

  (remove-db-files! :student 2)
  (remove-db-files!)
  :end
  )