(ns abantu.routes.api.spamtest
  (:require [ring.util.response :as res]
            [abantu.routes.openapi :as api]
            [abantu.db.honey :as hon])
  (:import [java.time Instant]))

(defn- random-timestamp []
  (let [base (.toEpochMilli (Instant/now))
        offset (long (* -30 24 60 60 1000 (rand)))]
    (+ base offset)))

(defn- random-exercises-completed []
  {:user-id (inc (rand-int 100))
   :exercise-id (inc (rand-int 200))
   :unit-id (inc (rand-int 50))
   :correct (rand-int 2)
   :timestamp (random-timestamp)
   :practice-session-id nil})

(defn- generate-events [n]
  (mapv (fn [_] (random-exercises-completed)) (range n)))

(defn- count-table [ds tname]
  (let [result (hon/execute! ds {:select [[[:count :*] :count]]
                                 :from tname}
                             {:ret :1})]
    (:count result)))

(defn- delete-oldest-half! [ds tname]
  (let [total (count-table ds tname)
        half (quot total 2)
        oldest (hon/find ds {:tname tname
                             :order-by [[:id :asc]]
                             :limit half
                             :ret :*})
        ids (mapv :id oldest)]
    (when (seq ids)
      (hon/delete! ds {:tname tname
                       :where [:in :id ids]}))
    (count ids)))

(defn get-spamtest
  {:summary "Stress test: bulk insert, delete half, read all"
   :responses (api/success [:map [:remaining :string]])}
  [{:keys [ds] :as _req}]
  (try
    (let [_inserts (doseq [_i (range 20)]
                    (hon/insert! ds {:tname :exercises-completed
                                     :values (generate-events 1)}))
          _deleted (delete-oldest-half! ds :exercises-completed)
          remaining (hon/find ds {:tname :exercises-completed})]
      (println "successfully completed stresstest!")
      (res/response {:remaining (count remaining)}))
    (catch Exception e
      (println "failed stresstest:(" \n (.getMessage e)))))
