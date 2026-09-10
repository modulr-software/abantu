(ns abantu.services.changes.core
  (:require [abantu.db.interface :as db]
            [abantu.services.courses.interface :as course]
            [abantu.services.courses.update :as course-update]
            [abantu.services.units.interface :as unit]
            [abantu.services.units.update :as unit-update]
            [abantu.services.exercises.interface :as exercise]
            [abantu.services.exercises.update :as exercise-update]
            [abantu.util :as util]
            [jsonista.core :as json]
            [abantu.db.util :as db.util]))

(defn- attach-courses [ds {:keys [id] :as version}]
  (assoc version :course-changes (db/find ds {:tname :course-changes
                                              :where [:= :version-id id]
                                              :order-by :id
                                              :ret :*})))

(defn- attach-units [ds {:keys [id] :as version}]
  (assoc version :unit-changes (db/find ds {:tname :unit-changes
                                            :where [:= :version-id id]
                                            :order-by :id
                                            :ret :*})))

(defn- attach-exercises [ds {:keys [id] :as version}]
  (assoc version :exercise-changes (db/find ds {:tname :exercise-changes
                                                :where [:= :version-id id]
                                                :order-by :id
                                                :ret :*})))

(defn -lookup [ds {:keys [_id _timestamp with-changes?] :as opts}]
  (cond->> (-> (db/find-one ds {:tname :versions
                                :where (util/eq-clauses (dissoc opts :with-changes?))})
               (util/parse-bool-keys [:applied]))
    with-changes? (attach-courses ds)
    with-changes? (attach-units ds)
    with-changes? (attach-exercises ds)))

(defn -find [ds {:keys [_course-id _label with-changes?] :as opts}]
  (cond->> (db/find ds {:tname :versions
                        :where (util/eq-clauses (dissoc opts :with-changes?))
                        :ret :*})
    true (mapv #(util/parse-bool-keys % [:applied]))
    with-changes? (mapv (comp (partial attach-courses ds)
                              (partial attach-units ds)
                              (partial attach-exercises ds)))))

(defn -all [ds {:keys [with-changes?] :as _opts}]
  (cond->> (db/find ds {:tname :versions
                        :ret :*})
    true (mapv #(util/parse-bool-keys % [:applied]))
    with-changes? (mapv (comp (partial attach-courses ds)
                              (partial attach-units ds)
                              (partial attach-exercises ds)))))

(defn- parse-change [{:keys [change-type change-data] :as change}]
  (merge change {:change-type (keyword change-type)
                 :change-data (->> {:decode-key-fn true}
                                   (json/object-mapper)
                                   (json/read-value change-data))}))

(defn- group-changes-by-uuid [changes]
  (->> changes
       (reduce (fn [acc {:keys [uuid]}]
                 (assoc acc uuid (filterv #(= (:uuid %) uuid) changes))) {})
       (map (fn [m] {:uuid (first m) :change (last m)}))))

(defn -exercises [ds {:keys [_version-id _timestamp] :as opts}]
  (db/find ds {:tname :exercise-changes
               :where (util/eq-clauses opts)
               :order-by :id
               :ret :*}))

(defn -units [ds {:keys [_version-id _timestamp] :as opts}]
  (->> (db/find ds {:tname :unit-changes
                    :where (util/eq-clauses opts)
                    :order-by :id
                    :ret :*})))

(defn -courses [ds {:keys [_version-id _timestamp] :as opts}]
  (db/find ds {:tname :course-changes
               :where (util/eq-clauses opts)
               :order-by :id
               :ret :*}))

(defn -add-version! [ds {:keys [label course-id] :as _payload}]
  (let [{:keys [id]} (db/insert! ds {:tname :versions
                                     :data {:label label
                                            :course-id course-id
                                            :timestamp (util/get-utc-timestamp-string)}
                                     :ret :1})]
    (-lookup ds {:id id})))

(defn -set-version-label! [ds {:keys [id label]}]
  (db/update! ds {:tname :versions
                  :data {:label label}
                  :where [:= :id id]}))

(defn -add-exercise-update! [ds {:keys [version-id change-type update]}]
  (let [change-type (str change-type)
        {:keys [uuid unit-uuid course-uuid]} update
        json (json/write-value-as-string (dissoc update :id :unit-id :course-id))]
    (db/insert! ds {:tname :exercise-changes
                    :data {:uuid uuid
                           :unit-uuid unit-uuid
                           :course-uuid course-uuid
                           :change-type change-type
                           :change-data json
                           :timestamp (util/get-utc-timestamp-string)
                           :version-id version-id}
                    :ret :1})))

(defn -add-unit-update! [ds {:keys [version-id change-type update]}]
  (let [change-type (str change-type)
        {:keys [uuid course-uuid]} update
        json (json/write-value-as-string (dissoc update :id :course-id :exercises))
        exercises (:exercises update)
        result (db/insert! ds {:tname :unit-changes
                               :data {:uuid uuid
                                      :course-uuid course-uuid
                                      :change-type change-type
                                      :change-data json
                                      :timestamp (util/get-utc-timestamp-string)
                                      :version-id version-id}
                               :ret :1})]
    result
    #_(when (seq exercises)
        (println
         (->> exercises
              (mapv (fn [exercise]
                      {:version-id version-id
                       :change-type change-type
                       :update (assoc exercise :unit-uuid uuid :course-uuid course-uuid)}))))
        (->> exercises
             (mapv (fn [exercise]
                     {:version-id version-id
                      :change-type change-type
                      :update (assoc exercise :unit-uuid uuid :course-uuid course-uuid)}))
             (mapv #(-add-exercise-update! ds %))
             (assoc (:change-data result) :exercises)
             (assoc result :change-data)))))

(defn -add-course-update! [ds {:keys [version-id change-type update]}]
  (let [change-type (str change-type)
        uuid (:uuid update)
        json (json/write-value-as-string (dissoc update :id :units))
        units (:units update)
        result (db/insert! ds {:tname :course-changes
                               :data {:uuid uuid
                                      :change-type change-type
                                      :change-data json
                                      :timestamp (util/get-utc-timestamp-string)
                                      :version-id version-id}
                               :ret :1})]
    result
    #_(when (seq units)
        (->> units
             (mapv (fn [unit]
                     {:version-id version-id
                      :change-type change-type
                      :update (assoc unit :course-uuid uuid)}))
             (mapv #(-add-unit-update! ds %))
             (assoc (:change-data result) :units)
             (assoc result :change-data)))))

(defn exercise-update [update {:keys [change-type change-data]}]
  (-> update
      (assoc :_op (cond
                    (= change-type :create) :create
                    (= change-type :delete) :delete
                    (not (= (:_op update) :create)) :update
                    :else (:_op update)))
      (exercise-update/apply {:type change-type
                              :payload change-data})))

(defn unit-update [update {:keys [change-type change-data]}]
  (-> update
      (assoc :_op (cond
                    (= change-type :create) :create
                    (= change-type :delete) :delete
                    (not (= (:_op update) :create)) :update
                    :else (:_op update)))
      (unit-update/apply {:type change-type
                          :payload change-data})))

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

(defn migrate-up! [ds {:keys [id applied course-id] :as _version}])

(comment
  (db/find (db.util/conn) {:tname :courses})
  (db/find (db.util/conn) {:tname :units})
  (db/find (db.util/conn) {:tname :exercises})
  (db/find (db.util/conn) {:tname :answers})

  (with-open [ds (db.util/conn :student 1)
              master-ds (db.util/conn)]
    (let [id 1
          course-id 1
          applied false

          course-changes (->> (-courses ds {:version-id id})
                              (mapv parse-change)
                              (group-changes-by-uuid)
                              (mapv #(stack-changes course-update %)))
          unit-changes (->> (-units ds {:version-id id})
                            (mapv parse-change)
                            (group-changes-by-uuid)
                            (mapv #(stack-changes unit-update %)))
          exercise-changes (->> (-exercises ds {:version-id id})
                                (mapv parse-change)
                                (group-changes-by-uuid)
                                (mapv #(stack-changes exercise-update %)))]

      (when (not applied)
        (run! #(apply-course-changes! master-ds %) course-changes)
        (run! #(apply-unit-changes! master-ds %) unit-changes)
        (run! #(apply-exercise-changes! master-ds %) exercise-changes)

        #_(apply-update (partial course-update! master-ds) course-changes)
        #_(db/update! ds {:tname :versions
                          :data {:applied 1}
                          :where [:= :id id]}))))
  ())
