(ns abantu.services.write-queue
  (:require
   [honey.sql.helpers :as hsql]
   [abantu.db.util :as db.util]
   [abantu.db.honey :as hon]
   [clj-oauth2.client :as oauth2]))

; QUEUE STUFFS

(def *q (atom clojure.lang.PersistentQueue/EMPTY))

(defn enq! [*q item]
  (swap! *q conj item)
  nil)

(defn deq! [q]
  (let [extracted-item (atom nil)]
    (swap! q (fn [q]
               (reset! extracted-item (peek q))
               (pop q)))
    @extracted-item))

; FRED STUFF
(defprotocol Fred
  (mutate! [this opts]
    "Push a new task to the task list to be executed")
  (query [this opts]
    "Execute a task immediately"))

(def *worker (atom false))

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

(defn worker-thunk [execute-fn]
  (reset! *worker true)
  (while (seq @*q)
    (when-let [{:keys [p] :as op} (deq! *q)]
      (->> (try
             (ok (execute-fn op))
             (catch Exception e
               (err e)))
           (deliver p))))
  (reset! *worker false))

(defn start-worker! [execute-fn]
  (future (worker-thunk execute-fn)))

(defn worker-alive? []
  @*worker)

(defn- ensure-worker! [execute-fn]
  (when-not (worker-alive?)
    (start-worker! execute-fn)))

; QUERY & MUTATION STUFF 

(defn- -mutate!
  "accepts honey-compatible sql mutations as 
  {:type :insert|update|delete 
  :ds ds
  :query sql}
  returns a promise that delivers the write result (or the Exception on failure)."
  [execute-fn sql-mutation]
  (let [p (promise)
        op (assoc sql-mutation :p p)]
    (enq! *q op)
    (ensure-worker! execute-fn)
    (unwrap-response @p)))


(defn- -query
  "accepts a honey-compatible sql query as 
  {:type :select 
  :ds ds 
  :query sql}
  returns a promise that delivers the query result (or the Exception on failure)"
  [execute-fn sql-query]
  (let [p (promise)]
    (->> (try
           (ok (execute-fn sql-query))
           (catch Exception e
             (err e)))
         (deliver p)
         (future))
    @p))

(defn create-fred
  "Create a new instance of Fred with the given executor function"
  [{:keys [execute-fn]}]
  (reify Fred
    (mutate! [_ opts]
      (-mutate! execute-fn opts))
    (query [_ opts]
      (-query execute-fn opts))))

(defn process-op [{:keys [_type ds query]}]
  (hon/execute! ds query {:ret :*}))


(comment
  ())
