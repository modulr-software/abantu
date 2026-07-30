(ns abantu.services.stats
  "Computed-from-events numbers. Reads student-side session/event rows
  and derives scalar metrics. Does not touch the master units/courses
  services — call these from handlers that already have a user id."
  (:require [honey.sql.helpers :as hsql]
            [abantu.db.honey :as hon]
            [abantu.db.util :as db.util]))

(defn- one-month-ago-utc
  "ISO-8601 UTC timestamp string for one month before now, matching the
  format written by util/get-utc-timestamp-string."
  []
  (-> (java.time.ZonedDateTime/now java.time.ZoneOffset/UTC)
      (.minusMonths 1)
      (.format (java.time.format.DateTimeFormatter/ofPattern
                "yyyy-MM-dd'T'HH:mm:ss'Z'"))))

(defn unit-progress
  "Returns a progress score in [0.0, 1.0] for `user-id` on `unit-id`,
  computed from session-answers within the last month:
  sum(correct) / count(*). 0.0 when there are no answer rows."
  [user-id unit-id]
  (with-open [ds (db.util/conn :student user-id)]
    (let [{:keys [total correct]}
          (hon/execute! ds
                        (-> (hsql/select [[:count :*] :total]
                                         [[:sum :correct] :correct])
                            (hsql/from :session-answers)
                            (hsql/where [:and
                                         [:= :unit-id unit-id]
                                         [:>= :ended-at (one-month-ago-utc)]]))
                        :ret :1)]
      (if (zero? (or total 0))
        0.0
        (double (/ (or correct 0) total))))))

(defn course-progress
  "Overall course progress in [0.0, 1.0]: the average of the :progress
  values on `units` (which must already carry :progress). 0.0 when no units."
  [units]
  (let [progresses (map :progress units)]
    (if (empty? progresses)
      0.0
      (double (/ (reduce + progresses) (count progresses))))))

(defn- duration-seconds
  "Seconds between two ISO-8601 UTC timestamp strings as written by
  util/get-utc-timestamp-string."
  [started-at ended-at]
  (.getSeconds
   (java.time.Duration/between
    (java.time.Instant/parse started-at)
    (java.time.Instant/parse ended-at))))

(defn- session-stats
  "Computes per-session stats: correct count, mistakes, total time in seconds."
  [ds {:keys [id started-at ended-at]}]
  (let [{:keys [total correct]}
        (hon/execute! ds
                      (-> (hsql/select [[:count :*] :total]
                                       [[:sum :correct] :correct])
                          (hsql/from :session-answers)
                          (hsql/where [:= :session-id id]))
                      :ret :1)
        correct (or correct 0)
        total   (or total 0)]
    {:correct    correct
     :mistakes   (- total correct)
     :total-time (duration-seconds started-at ended-at)}))

(defn session-delta
  "Compares the just-completed session `session-id` for `user-id` against
  the most recent prior completed session for the same unit. Returns:
    {:has-previous-session true
     :mistakes-diff int :correct-diff int :time-diff int}  ; when a prior exists
    {:has-previous-session false
     :mistakes int :correct int :total-time int}           ; first session ever
  Handler merges the map straight into the response."
  [user-id session-id]
  (with-open [ds (db.util/conn :student user-id)]
    (let [current (hon/find-one ds {:tname :sessions
                                    :where [:= :id session-id]})
          prev    (hon/execute! ds
                                (-> (hsql/select :*)
                                    (hsql/from :sessions)
                                    (hsql/where [:and
                                                 [:= :unit-id (:unit-id current)]
                                                 [:= :completed 1]
                                                 [:not= :id session-id]])
                                    (hsql/order-by [:ended-at :desc])
                                    (hsql/limit 1))
                                :ret :1)
          new-stats (session-stats ds current)]
      (if prev
        (let [prev-stats (session-stats ds prev)]
          {:has-previous-session true
           :mistakes-diff (- (:mistakes new-stats) (:mistakes prev-stats))
           :correct-diff  (- (:correct new-stats) (:correct prev-stats))
           :time-diff     (- (:total-time new-stats) (:total-time prev-stats))})
        (assoc new-stats :has-previous-session false
                           :mistakes-diff (:mistakes new-stats)
                           :correct-diff  (:correct new-stats)
                           :time-diff     (:total-time new-stats))))))
