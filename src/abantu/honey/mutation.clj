(ns abantu.honey.mutation
  (:require [honey.sql.helpers :as hsql]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))

(defn insert!
  "Constructs Honey DSL to insert a single record or a set of records into a 
  table. records passed in map form where the keys can be kebab-case keywords. 
  All keys are converted to snake_case strings before executing prepared 
  statements."
  [{:keys [tname data values]}]
  (let [values' (or data values)
        multi? (vector? values')
        vals (if multi? values' [values'])]
    (-> (hsql/insert-into (csk/->snake_case_keyword tname))
        (hsql/values vals)
        (hsql/returning :*))))

(defn delete!
  "Constructs Honey DSL to delete a record or set of records that match a 
  predicate where clause. The where clause uses the same data dsl as honey sql"
  [{:keys [tname where]}]
  (-> (hsql/delete-from (csk/->snake_case_keyword tname))
      (hsql/where
       (or (cske/transform-keys
            csk/->snake_case_keyword where)
           []))))

(defn update!
  "Constructs Honey DSL to update a record or set of records that match a 
  predicate where clause. The where clause uses the same data dsl as honey sql. 
  All values to apply are supplied in a map where the keys are kebab-case column 
  names. The keys are automatically converted to snake_case strings before 
  executing the prepared statement."
  [{:keys [tname where data values]}]
  (-> (hsql/update (csk/->snake_case_keyword tname))
      (hsql/set
       (cske/transform-keys
        csk/->snake_case_keyword (or data values)))
      (hsql/where
       (or
        (cske/transform-keys
         csk/->snake_case_keyword where)
        []))))
