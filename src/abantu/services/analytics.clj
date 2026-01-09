(ns abantu.services.analytics
  (:require [honey.sql.helpers :as hsql]
            [abantu.db.honey :as hon]
            [abantu.db.interface :as db]
            [abantu.util :as util]))

(defn insert-completion!
  [ds {:keys [_user-id _exercise-id _unit-id _correct _timestamp] :as exercise}]
  (-> (db/insert! ds {:tname :exercises-completed
                      :data exercise
                      :ret :1})
      (util/parse-bool-keys [:correct])))

(defn insert-from-practice-session!
  [ds {:keys [exercises user-id timestamp]}]
  (->> exercises
       (mapv #(assoc % :user-id user-id :timestamp timestamp))
       (mapv (partial insert-completion! ds))))

(defn query
  "Generic select query function for returning analytics data from the exercises-completed table"
  [ds {:keys [select order-by group-by limit user-id exercise-id unit-id correct min-date max-date where ret]}]
  (let [clauses (cond-> {}
                  (some? where) (merge where)
                  (some? user-id) (hsql/where [:= :user-id user-id])
                  (some? exercise-id) (hsql/where [:= :exercise-id exercise-id])
                  (some? unit-id) (hsql/where [:= :unit-id unit-id])
                  (some? correct) (hsql/where [:= :correct (> correct 0)])
                  (and (some? min-date) (nil? max-date)) (hsql/where [:>= :timestamp min-date])
                  (and (some? max-date) (nil? min-date)) (hsql/where [:<= :timestamp max-date])
                  (and (some? min-date) (some? max-date)) (hsql/where [:between :timestamp min-date max-date])
                  (some? select) (assoc :select select)
                  (nil? select) (merge {:select [[[:count :*] :total]]})
                  (some? limit) (hsql/limit limit)
                  (some? order-by) (merge order-by)
                  (some? group-by) (merge group-by))]
    (-> (hon/execute!
         ds
         (merge {:from [:exercises-completed]}
                clauses)
         {:ret (if ret ret :*)}))))

(defn- delete-all! [ds]
  (db/delete! ds {:tname :exercises-completed
                  :ret :*}))

(comment
  (require '[abantu.db.util :as db.util])
  (def ds (db.util/conn))

  (delete-all! ds)

  (insert-completion!
   ds
   {:user-id 1
    :exercise-id 1
    :unit-id 1
    :correct true
    :timestamp (System/currentTimeMillis)})

  (insert-from-practice-session!
   ds
   {:user-id 1
    :timestamp (System/currentTimeMillis)
    :exercises [{:exercise-id 7
                 :unit-id 2
                 :correct true}
                {:exercise-id 14
                 :unit-id 3
                 :correct false}]})

  (def test-config
    {:users 1
     :units 4
     :exercises 400})

  (defn seed-completions [{:keys [users units exercises]}]
    (doseq [_ (range exercises)]
      (insert-completion! ds {:user-id (inc (rand-int users))
                              :exercise-id (inc (rand-int exercises))
                              :unit-id (inc (rand-int units))
                              :correct (inc (rand-int 2))
                              :timestamp (System/currentTimeMillis)})))

  (seed-completions test-config)

  (query ds {:select [:*]})

  ())
