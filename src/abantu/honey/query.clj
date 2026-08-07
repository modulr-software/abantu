(ns abantu.honey.query
  (:require [honey.sql.helpers :as hsql]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))

(defn find
  "Constructs Honey DSL find one or find all for a given table name and where 
  clause. The where clause follows the same data DSL as honeysql. Automatically 
  transforms kebab case keys into snake case for sql. e.g. :provider-id becomes 
  \"provider_id\" when honey sql prepares the statement in execute!"
  [{:keys [tname where order-by limit]}]
  (-> (hsql/select :*)
      (hsql/from (csk/->snake_case_keyword tname))
      (merge (if (some? order-by) {:order-by order-by} {}))
      (merge (if (some? limit) (hsql/limit limit) {}))
      (hsql/where
       (or (cske/transform-keys
            csk/->snake_case_keyword where)
           []))))
