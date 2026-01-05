(ns abantu.db.honey
  (:require [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [honey.sql :as sql]
            [honey.sql.helpers :as hsql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [abantu.db.util :as db.util]))

(defn execute!
  "computes a prepared statement for an sql map and executes select one
  or select all. returns results as unqualified lower maps by default."
  [ds sqlmap & {:keys [ret exec-opts]}]
  (prn "sqlmap" sqlmap)
  (assert (and (some? ds) (some? sqlmap) (or (some? ret) (nil? ret))))
  (let [ps (sql/format sqlmap)
        exec-opts' (merge
                    {:builder-fn rs/as-unqualified-lower-maps}
                    exec-opts)
        result (cske/transform-keys
                csk/->kebab-case-keyword
                (jdbc/execute! ds ps exec-opts'))]
    (cond
      (= ret :1) (first result)
      (= ret :*) result
      :else nil)))

(defn find
  "does find one or find all for a given table name and where clause. The where
  clause follows the same data DSL as honeysql. Automatically transforms kebab
  case keys into snake case for sql. e.g. :provider-id becomes \"provider_id\"
  when honey sql prepares the statement in execute!"
  [ds {:keys [tname where order-by limit ret]}]
  (execute! ds
            (-> (hsql/select :*)
                (hsql/from (csk/->snake_case_keyword tname))
                (merge (if (some? order-by) {:order-by order-by} {}))
                (merge (if (some? limit) (hsql/limit limit) {}))
                (hsql/where
                 (or (cske/transform-keys
                      csk/->snake_case_keyword where)
                     [])))
            :ret ret))

(defn find-one [ds opts]
  (->> {:ret :1}
       (merge opts)
       (find ds)))

(defn insert!
  "inserts a single record or a set of records into a table. records passed in 
  map form where the keys can be kebab-case keywords. all keys are converted 
  to snake_case strings before executing prepared statements."
  [ds {:keys [tname data values ret]}]
  (let [values' (or data values)
        multi? (vector? values')
        vals (if multi? values' [values'])]
    (execute! ds
              (-> (hsql/insert-into (csk/->snake_case_keyword tname))
                  (hsql/values vals)
                  (hsql/returning :*))
              :ret ret)))

(defn delete!
  "deletes a record or set of records that match a predicate where clause. the where
  clause uses the same data dsl as honey sql"
  [ds {:keys [tname where ret]}]
  (execute! ds
            (-> (hsql/delete-from (csk/->snake_case_keyword tname))
                (hsql/where
                 (or (cske/transform-keys
                      csk/->snake_case_keyword where)
                     [])))
            :ret ret))

(defn exists? [ds opts]
  (->> opts
       (find-one ds)
       (some?)))

(defn update!
  "updates a record or set of records that match a predicate where clause. the where
  clause uses the same data dsl as honey sql. All values to apply are supplied in a 
  map where the keys are kebab-case column names. The keys are automatically 
  converted to snake_case strings before executing the prepared statement."
  [ds {:keys [tname where data values ret]}]
  (execute! ds
            (-> (hsql/update (csk/->snake_case_keyword tname))
                (hsql/set
                 (cske/transform-keys
                  csk/->snake_case_keyword (or data values)))
                (hsql/where
                 (or
                  (cske/transform-keys
                   csk/->snake_case_keyword where)
                  [])))
            :ret ret))

(comment
  (hsql/where :or [:= :id 1] [:= :id 2])

  (def ds (db.util/conn :master))

  (def unique-classes
    (->>
     (find ds {:tname :vocab
               :ret :*})
     (mapv :type)
     (filterv identity)
     (set)))

  unique-classes

  (->>
   (find ds {:tname :vocab
             :where [:= :type "adjective"]
             :ret :*})
   (mapv :xhosa)
   )

  
  (insert! ds {:tname :vocab
               :values (-> (slurp "resources/dict.json")
                           (clojure.data.json/read-str {:key-fn keyword}))
               :ret :*})


  (->> {:tname :vocab
        :ret :*}
       (find ds)
       (filterv #(clojure.string/includes? (:type %) "interjection"))
       (mapv #(update! ds {:tname :vocab
                           :where (:id %)
                           :values {:type "interjection"}
                           :ret :*})))
  
  (find ds {:tname :units
            :ret :*})
  
  (find ds {:tname :users
            :ret :*})

  (find ds {:tname :courses
            :ret :*})


  ()
  )
