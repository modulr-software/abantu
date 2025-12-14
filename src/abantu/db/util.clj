(ns abantu.db.util
  (:require [abantu.config :as conf]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

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
  (let [conn (-> {:dbtype (conf/read-value :database :type)}
                 (merge {:dbname (db-path dbname)})
                 (jdbc/get-connection))]
    (jdbc/execute! conn ["PRAGMA journal_mode = WAL;"])
    (jdbc/execute! conn ["PRAGMA synchronous = NORMAL;"])
    (jdbc/with-options conn {:builder-fn rs/as-unqualified-lower-maps})
    conn))

(defn conn
  ([]
   (conn :master))
  ([db-type]
   (assert (= db-type :master))
   (-conn (db-name db-type)))
  ([db-type id]
   (assert (or (= db-type :bundle) (= db-type :creator)))
   (-conn (db-name db-type id))))
