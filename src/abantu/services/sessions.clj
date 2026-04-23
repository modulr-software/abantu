(ns abantu.services.sessions
  (:require [abantu.db.honey :as hon]
            [abantu.util :as util]
            [clojure.string :as str]
            [abantu.db.util :as db.util]))

(defn start-session!
  "Accepts a db connection and a unit map.
  Inserts a session start event containing unit id of the unit for which the session was started."
  [ds {:keys [id course-id] :as _unit}]
  (hon/insert! ds {:tname :sessions
                   :data {:unit-id id
                          :course-id course-id
                          :started-at (util/get-utc-timestamp-string)
                          :completed 0}}))

(defn- insert-session-answer! [ds {:keys [unit-id course-id exercise-id answer correct started-at ended-at session-id]}]
  (println (hon/find ds {:tname :sqlite_schema
                         :ret :*}))
  (let [answer (if (vector? answer)
                 (str/join ";;" answer)
                 answer)]
    (hon/insert! ds {:tname :session-answers
                     :data {:unit-id unit-id
                            :course-id course-id
                            :exercise-id exercise-id
                            :answer answer
                            :correct (if correct 1 0)
                            :started-at started-at
                            :ended-at ended-at
                            :session-id session-id}})))

(defn end-session!
  "Accepts a db connection, session id, a unit map and a list of maps containing 
  exercise-ids, given answer and correct boolean.
  Inserts session-end events containing exercise ids and whether they were correct."
  [ds session-id {:keys [id course-id] :as _unit} exercises]
  (run!
   #(insert-session-answer! ds (merge % {:unit-id id
                                         :course-id course-id
                                         :session-id session-id}))
   exercises)
  (hon/update! ds {:tname :sessions
                   :where [:= :id session-id]
                   :data {:completed 1
                          :ended-at (util/get-utc-timestamp-string)}}))

