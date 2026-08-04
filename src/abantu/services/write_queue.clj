(ns abantu.services.write-queue
  (:require
   [honey.sql.helpers :as hsql]
   [camel-snake-kebab.core :as csk]
   [abantu.db.util :as db.util]
   [abantu.db.honey :as hon]
   [camel-snake-kebab.extras :as cske]
   [clojure.set :as set]))

; QUEUE STUFFS

(def q (atom clojure.lang.PersistentQueue/EMPTY))

(defn enq! [q item]
  (swap! q conj item)
  nil)

(defn deq! [q]
  (let [extracted-item (atom nil)]
    (swap! q (fn [q]
               (reset! extracted-item (peek q))
               (pop q)))
    @extracted-item))

; FRED STUFF

(def *worker (atom false))

(defn process-op [{:keys [_type ds query]}]
  (hon/execute! ds query {:ret :*}))

(defn- unwrap-response [{:keys [result err] :as _response}]
  (when err
    (throw err))
  result)

(defn- ok [value]
  {:result value
   :err nil})

(defn- err [value]
  {:result nil
   :err value})

(defn worker-thunk []
  (reset! *worker true)
  (while (seq @q)
    (when-let [{:keys [p] :as op} (deq! q)]
      (->> (try
             (ok (process-op op))
             (catch Exception e
               (err e)))
           (deliver p))))
  (reset! *worker false))

(defn start-worker! []
  (future (worker-thunk)))

(defn worker-alive? []
  @*worker)

(defn- ensure-worker! []
  (when-not (worker-alive?)
    (start-worker!)))

; QUERY & MUTATION STUFF 

(defn mutate!
  "accepts honey-compatible sql mutations as 
  {:type :insert|update|delete 
  :ds ds
  :query sql}
  returns a promise that delivers the write result (or the Exception on failure)."
  [sql-mutation]
  (let [p (promise)
        op (assoc sql-mutation :p p)]
    (enq! q op)
    (ensure-worker!)
    (unwrap-response @p)))

(defn query!
  "accepts a honey-compatible sql query as 
  {:type :select 
  :ds ds 
  :query sql}
  returns a promise that delivers the query result (or the Exception on failure)"
  [sql-query]
  (let [p (promise)]
    (->> (try
           (ok (process-op sql-query))
           (catch Exception e
             (err e)))
         (deliver p)
         (future))
    @p))

(defn- insert! [ds {:keys [tname data values]}]
  (let [values' (or data values)
        multi? (vector? values')
        vals (if multi? values' [values'])]
    (mutate!
     {:type :insert
      :ds ds
      :query (-> (hsql/insert-into (csk/->snake_case_keyword tname))
                 (hsql/values vals)
                 (hsql/returning :*))})))

(defn- find [ds {:keys [tname where order-by limit]}]
  (query!
   {:type :select
    :ds ds
    :query (-> (hsql/select :*)
               (hsql/from (csk/->snake_case_keyword tname))
               (merge (if (some? order-by) {:order-by order-by} {}))
               (merge (if (some? limit) (hsql/limit limit) {}))
               (hsql/where
                (or (cske/transform-keys
                     csk/->snake_case_keyword where)
                    [])))}))

(comment

  (enq! q :first)
  (enq! q :second)
  (enq! q :third)
  (deq! q)
  (peek @q)

  (try
    (insert! (db.util/conn) {:tname :exercises-completed
                             :data {:user-id 1
                                    :unit-id 1
                                    :exercise-id 1
                                    :timestamp 0
                                    :practice-session-id 1}})
    (catch Exception e
      (println "o nei we got an error: " (.getMessage e))))

  (find (db.util/conn) {:tname :exercises-completed
                        :ret :*})

  (count
   (hon/find (db.util/conn) {:tname :exercises-completed
                             :ret :*}))

  ;; fire 5 inserts in parallel to exercise the queue
  (time
   (let [ds (db.util/conn)]
     (doall
      (pmap #(insert! ds {:tname :exercises-completed
                          :data {:useraoensuthaoeu 1
                                 :unit-id 1
                                 :exercise-id %
                                 :timestamp 0
                                 :practice-session-id 1}})
            (range 5)))))

  (defn c []
    (let [p (promise)]
      (future (do (Thread/sleep 2000)
                  (deliver p false)))
      p))

  (defn b []
    (let [bp (promise)
          p (c)]
      (if @p
        (deliver bp @p)
        (throw (ex-info "o nei we got false" {})))
      bp))

  (defn a []
    (let [p (b)]
      (println "success: " @p)))

  (a)

  (let [yomama (promise)
        yeet (assoc {} :p yomama)]
    (future @yomama)
    (future
      (Thread/sleep 2000)
      (deliver yomama (ex-info "o nei" {}))))

  ;; create a fresh test_<id> db (schema cloned from master), then drive
  ;; 5 inserts through mutate! with hsql-built sql-mutations and assert
  ;; the rows land in enqueue order (autoincrement :id asc => exercise-id 0..4).
  (let [id 1
        _   (db.util/create-test-db! id)
        ds  (db.util/conn :test id)]
    (try
      (reset! q clojure.lang.PersistentQueue/EMPTY)
      (reset! *worker false)
      (doseq [i (range 5)]
        (mutate!
         {:type :insert
          :ds ds
          :query (-> (hsql/insert-into :exercises_completed)
                     (hsql/values [{:user-id 1
                                    :unit-id 1
                                    :exercise-id i
                                    :timestamp 0
                                    :practice-session-id 1}])
                     (hsql/returning :*))}))
      (let [got (->> (hon/find ds {:tname :exercises-completed
                                   :order-by [:id]
                                   :ret :*})
                     (mapv :exercise-id))]
        (prn "ordered exercise-ids:" got)
        (assert (= (vec (range 5)) got)
                (str "expected 0..4 in order, got " got))
        :ok)
      (finally
        (db.util/remove-db-files! :test id))))

  ;; fire 1000 inserts concurrently via pmap. enqueue order is now
  ;; nondeterministic (futures race to enq!), so we assert count and the
  ;; full set of exercise-ids rather than order — the queue's job here is
  ;; to land every write safely under contention, not to preserve order.
  (let [id 2
        _   (db.util/create-test-db! id)
        ds  (db.util/conn :test id)]
    (try
      (reset! q clojure.lang.PersistentQueue/EMPTY)
      (reset! *worker false)
      (time
       (doall
        (pmap #(mutate!
                {:type :insert
                 :ds ds
                 :query (-> (hsql/insert-into :exercises_completed)
                            (hsql/values [{:user-id 1
                                           :unit-id 1
                                           :exercise-id %
                                           :timestamp 0
                                           :practice-session-id 1}])
                            (hsql/returning :*))})
              (range 1000))))
      (let [rows (hon/find ds {:tname :exercises-completed
                               :ret :*})
            ids  (set (map :exercise-id rows))]
        (prn "rows:" (count rows))
        (assert (= 1000 (count rows))
                (str "expected 1000 rows, got " (count rows)))
        (assert (= (set (range 1000)) ids)
                (str "expected exercise-ids 0..999, missing " (sort (set/difference (set (range 1000)) ids))))
        :ok)
      (finally
        (db.util/remove-db-files! :test id))))
  ())
