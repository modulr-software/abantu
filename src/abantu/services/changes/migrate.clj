(ns abantu.services.changes.migrate
  (:require [abantu.services.changes.core :as core]
            [abantu.services.exercises.update :as exercise-update]
            [abantu.services.units.update :as unit-update]
            [abantu.services.courses.update :as course-update]
            [abantu.services.exercises.interface :as exercise]
            [abantu.services.courses.interface :as course]
            [abantu.services.units.interface :as unit]
            [abantu.db.interface :as db]
            [abantu.db.util :as db.util]))

(defn exercise-update [update {:keys [change-type change-data]}]
  (-> update
      (assoc :_op (cond
                    (= change-type :create) :create
                    (= change-type :delete) :delete
                    (not (= (:_op update) :create)) :update
                    :else (:_op update)))
      (exercise-update/apply {:type change-type
                              :payload change-data})
      (assoc :unit-uuid (:unit-uuid change-data) :course-uuid (:course-uuid change-data))))

(defn unit-update [update {:keys [change-type change-data]}]
  (-> update
      (assoc :_op (cond
                    (= change-type :create) :create
                    (= change-type :delete) :delete
                    (not (= (:_op update) :create)) :update
                    :else (:_op update)))
      (unit-update/apply {:type change-type
                          :payload change-data})
      (assoc :course-uuid (:course-uuid change-data))))

(defn course-update [update {:keys [change-type change-data]}]
  (-> update
      (assoc :_op (cond
                    (= change-type :create) :create
                    (= change-type :delete) :delete
                    (not (= (:_op update) :create)) :update
                    :else (:_op update)))
      (course-update/apply {:type change-type
                            :payload change-data})))

(defn apply-update
  ([apply-fn changes] (apply-update apply-fn {} changes))
  ([apply-fn update changes]
   (let [change (first changes)
         update (apply-fn update change)]
     (if (seq (rest changes))
       (apply-update apply-fn update (rest changes))
       update))))

(defn stack-changes [update-fn {:keys [uuid change]}]
  (let [stacked (apply-update update-fn change)]
    {:uuid uuid
     :op (:_op stacked)
     :change (dissoc stacked :_op)}))

(defn apply-exercise-changes! [ds {:keys [uuid op change]}]
  (let [em (exercise/use-mutation ds)
        course (course/lookup (course/use-query ds) {:uuid (:course-uuid change)})
        unit (unit/lookup (unit/use-query ds) {:uuid (:unit-uuid change)})
        change (-> (assoc change :course-id (:id course) :unit-id (:id unit))
                   (dissoc :course-uuid :unit-uuid))]
    (cond
      (= op :create)
      (exercise/create em change)

      (= op :update)
      (do
        (db/update! ds {:tname :exercises
                        :data (dissoc change :answers :options)
                        :where [:= :uuid uuid]})
        (exercise/set-answers em {:uuid uuid
                                  :answers (:answers change)})
        (exercise/set-options em {:uuid uuid
                                  :options (:options change)}))

      (= op :delete)
      (exercise/delete em {:uuid uuid}))))

(defn apply-unit-changes! [ds {:keys [uuid op change]}]
  (let [course (course/lookup (course/use-query ds) {:uuid (:course-uuid change)})
        change (-> (assoc change :course-id (:id course))
                   (dissoc :course-uuid))]
    (cond
      (= op :create)
      (unit/create (unit/use-mutation ds) (assoc change :exercises []))

      (= op :update)
      (db/update! ds {:tname :units
                      :data change
                      :where [:= :uuid uuid]})

      (= op :delete)
      (unit/delete (unit/use-mutation ds) {:uuid uuid}))))

(defn apply-course-changes! [ds {:keys [uuid op change]}]
  (cond
    (= op :create)
    (course/create (course/use-mutation ds) (assoc change :units []))

    (= op :update)
    (db/update! ds {:tname :courses
                    :data change
                    :where [:= :uuid uuid]})

    (= op :delete)
    (course/delete (course/use-mutation ds) {:uuid uuid})))

(defn migrate-up! [ds {:keys [id timestamp] :as _version}]
  (with-open [master-ds (db.util/conn)]
    (let [course-changes (->> (core/-find-course-changes ds {:from timestamp})
                              (mapv #(stack-changes course-update %)))
          unit-changes (->> (core/-find-unit-changes ds {:from timestamp})
                            (mapv #(stack-changes unit-update %)))
          exercise-changes (->> (core/-find-exercise-changes ds {:from timestamp})
                                (mapv #(stack-changes exercise-update %)))]

      (run! #(apply-course-changes! master-ds %) course-changes)
      (run! #(apply-unit-changes! master-ds %) unit-changes)
      (run! #(apply-exercise-changes! master-ds %) exercise-changes)

      (db/update! ds {:tname :versions
                      :data {:applied 1}
                      :where [:= :id id]}))))

(comment
  (def ds (db.util/conn :student 1))

  (db/find (db.util/conn) {:tname :courses})
  (db/find (db.util/conn) {:tname :units})
  (db/find (db.util/conn) {:tname :exercises})
  (db/find (db.util/conn) {:tname :answers})

  (migrate-up! ds (core/-lookup ds {:id 2}))

  (with-open [master-ds (db.util/conn)]
    (let [{:keys [timestamp]} (db/find-one ds {:tname :versions
                                               :where [:= :id 1]})
          course-changes (->> (core/-courses ds {:from timestamp}))]
      course-changes))

  ())
