(ns abantu.db.interface
  (:require [abantu.db.util :as db.util]
            [abantu.db.honey :as hon]))

(defn ds [dbname]
  (db.util/conn dbname))

(defn db-name [& args]
  (apply db.util/db-name args))

(defn execute! [ds opts]
  (hon/execute! ds opts))

(defn find [ds {:keys [_tname _where _ret] :as opts}]
  (hon/find ds opts))

(defn find-one [ds opts]
  (hon/find-one ds opts))

(defn update! [ds opts]
  (hon/update! ds opts))

(defn delete! [ds opts]
  (hon/delete! ds opts))

(defn insert! [ds opts]
  (hon/insert! ds opts))

(defn exists? [ds opts]
  (hon/exists? ds opts))

