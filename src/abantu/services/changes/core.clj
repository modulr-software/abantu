(ns abantu.services.changes.core
  (:require [abantu.db.interface :as db]
            [abantu.util :as util]
            [jsonista.core :as json]
            [abantu.db.util :as db.util]))

(defn- parse-change [{:keys [change-type change-data] :as change}]
  (merge change {:change-type (keyword change-type)
                 :change-data (->> {:decode-key-fn true}
                                   (json/object-mapper)
                                   (json/read-value change-data))}))

(defn- group-changes-by-uuid [changes]
  (->> changes
       (reduce (fn [acc {:keys [uuid]}]
                 (assoc acc uuid (filterv #(= (:uuid %) uuid) changes))) {})
       (mapv (fn [m] {:uuid (first m) :change (last m)}))))

(defn -find-exercise-changes [ds {:keys [from to] :as _opts}]
  (->> (db/find ds {:tname :exercise-changes
                    :where (cond
                             (and from to) [:between :timestamp from to]
                             from [:>= :timestamp from]
                             to [:<= :timestamp to])
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)))

(defn -find-unit-changes [ds {:keys [from to] :as _opts}]
  (->> (db/find ds {:tname :unit-changes
                    :where (cond
                             (and from to) [:between :timestamp from to]
                             from [:>= :timestamp from]
                             to [:<= :timestamp to])
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)))

(defn -find-course-changes [ds {:keys [from to] :as _opts}]
  (->> (db/find ds {:tname :course-changes
                    :where (cond
                             (and from to) [:between :timestamp from to]
                             from [:>= :timestamp from]
                             to [:<= :timestamp to])
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)))

(defn- attach-courses [ds {:keys [timestamp] :as version}]
  (assoc version :course-changes (-find-course-changes ds {:to timestamp})))

(defn- attach-units [ds {:keys [timestamp] :as version}]
  (assoc version :unit-changes (-find-unit-changes ds {:to timestamp})))

(defn- attach-exercises [ds {:keys [timestamp] :as version}]
  (assoc version :exercise-changes (-find-exercise-changes ds {:to timestamp})))

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

(comment
  (def ds (db.util/conn :student 1))

  (db/find (db.util/conn) {:tname :courses})
  (db/find (db.util/conn) {:tname :units})
  (db/find (db.util/conn) {:tname :exercises})
  (db/find (db.util/conn) {:tname :answers})

  (with-open [master-ds (db.util/conn)]
    (let [{:keys [timestamp]} (db/find-one ds {:tname :versions
                                               :where [:= :id 1]})
          course-changes (->> (-find-course-changes ds {:from timestamp}))]
      course-changes))

  ())
