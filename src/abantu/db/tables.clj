(ns abantu.db.tables
  (:require [abantu.db.honey :as hon]
            [honey.sql.helpers :as hsql]))

(defn create-table-sql
  "returns a honey data DSL structure for creating a table tname
  with columns specified. The column specification is a set of honey sql
  dsl specifications. The tname can be anything that parses to a clojure keyword.
  All create table sql structures are generated with a qualifier of 'if not exists'."
  [tname & columns]
  (->> columns
       (concat [(hsql/create-table (keyword tname) :if-not-exists)])
       (apply hsql/with-columns)))

(defn qualify
  "returns a qualified symbol ns/tname"
  [ns tname]
  (symbol (str (name ns) "/" (name tname))))

(defn resolve-sql-def
  "Given a namespace ns and a keyword table name tname, this function
  resolves the value at the symbol ns/tname assuming it has been defined.
  both ns and tname can be keywords, but they must match the exact values
  required to produce a resolvable symbol.
  NOTE: do not call this function unless the symbol you are resolving is defined."
  [ns tname]
  @(resolve (qualify ns tname)))

(defn create-table!
  "Given keywords ns and table, parses the keywords into a
  resolvable keyword, resolves the symbol to retrieve defined
  sql create table honey statements, prepares jdbc statements from them,
  and executes with next.jdbc, returning the result of the execution."
  [ds ns tname]
  (->> (resolve-sql-def ns tname)
       (hon/execute! ds)))

(defn create-tables!
  "Like create-table! but accepts a vector of keywords for table names
  and runs create-table with ns on every table name keyword in the vector."
  [ds ns tables]
  (mapv #(create-table! ds ns %)
        tables))

(defn tables
  "returns all current tables in a sqlite datasource"
  [ds]
  (->> {:tname :sqlite-master
        :where [:and [:= :type "table"] [:<> :name "sqlite_sequence"]]
        :ret :*}
       (hon/find ds)))

(defn table-name
  "return the name of a table record"
  [table]
  (:name table))

(defn table-names
  "retrieves and returns all table names for an sqlite datasource"
  [ds]
  (->> (tables ds)
       (mapv table-name)))

(defn table-id
  "Returns valid honey data DSL for defining a table id column
  with defaults: integer primary key autoincrement. Can be used
  with (create-table-sql) to simplify creating table ids."
  []
  [:id :integer [:primary-key] :autoincrement])

(defn drop-table-sql
  "returns a honey data DSL structure for dropping a table tname"
  [tname]
  (hsql/drop-table tname))

(defn drop-table!
  "given a table tname, this function drops the table tname from 
  the provided datasource"
  [ds tname]
  (->> (drop-table-sql tname)
       (hon/execute! ds)))

(defn drop-tables!
  "this function is similar to drop-table!, except it can drop 
  multiple tables provided as a vector of table names"
  [ds tnames]
  (doseq [table-name tnames]
    (drop-table! ds table-name)))

(defn drop-all-tables!
  "drops all the tables in the provided sqlite datasource"
  [ds]
  (drop-tables! ds (mapv keyword (table-names ds))))

(defn foreign-key
  "for a column c, foreign table name ft and foreign column name fc,
  return a valid honey data DSL column definition for a foreign key. Can
  be used with (create-table-sql) to simplify creating foreign keys"
  [c ft fc]
  [[:foreign-key c] [:references ft fc]])

(comment
  (require '[abantu.db.util :as db.util])
  (tables (db.util/conn :master))
  (table-names (db.util/conn :master))
  (with-redefs [hon/execute! (fn [_ sql] sql)]
    (create-tables! nil :abantu.db.master [:users]))

  (create-tables!
   (db.util/conn :master)
   :abantu.db.master
   [:users
    :cadences
    :sectors
    :businesses])

  (drop-tables!
   (db.util/conn :master)
   [:users
    :sectors
    :cadences])

  (drop-all-tables! (db.util/conn :master))

  (require '[honey.sql :as sql])
  (->
   (create-table-sql
    :users
    [:id :integer [:primary-key] :autoincrement]
    [:email :text])
   (sql/format))

  ())
