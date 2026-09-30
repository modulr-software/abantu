(ns abantu.services.changes.core
  (:require [abantu.db.interface :as db]
            [abantu.util :as util]
            [jsonista.core :as json]
            [honey.sql :as h]))

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

(defn -find-exercise-changes [ds {:keys [from to change-type change-data] :as _opts}]
  (->> (db/find ds {:tname :exercise-changes
                    :where (cond
                             (and from to) [:between :timestamp from to]
                             from [:>= :timestamp from]
                             to [:<= :timestamp to]
                             change-type [:= :change-type change-type]
                             change-data [:like :change-data (str "%" change-data "%")])
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)))

(defn -find-unit-changes [ds {:keys [from to change-type change-data] :as _opts}]
  (->> (db/find ds {:tname :unit-changes
                    :where (cond
                             (and from to) [:between :timestamp from to]
                             from [:>= :timestamp from]
                             to [:<= :timestamp to]
                             change-type [:= :change-type change-type]
                             change-data [:like :change-data (str "%" change-data "%")])
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)))

(defn -find-course-changes [ds {:keys [from to change-type change-data] :as _opts}]
  (->> (db/find ds {:tname :course-changes
                    :where (cond
                             (and from to) [:between :timestamp from to]
                             from [:>= :timestamp from]
                             to [:<= :timestamp to]
                             change-type [:= :change-type change-type]
                             change-data [:like :change-data (str "%" change-data "%")])
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)))

(defn -lookup-exercise-change [ds {:keys [_uuid _change-type _timestamp] :as opts}]
  (->> (db/find ds {:tname :exercise-changes
                    :where (h/map= opts)
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)
       first))

(defn -lookup-unit-change [ds {:keys [_uuid _change-type _timestamp] :as opts}]
  (->> (db/find ds {:tname :unit-changes
                    :where (h/map= opts)
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)
       first))

(defn -lookup-course-change [ds {:keys [_uuid _change-type _timestamp] :as opts}]
  (->> (db/find ds {:tname :course-changes
                    :where (h/map= opts)
                    :order-by :id
                    :ret :*})
       (mapv parse-change)
       (group-changes-by-uuid)
       first))

(defn -add-exercise-update! [ds {:keys [change-type update]}]
  (let [change-type (if (keyword? change-type) (name change-type) (str change-type))
        {:keys [uuid unit-uuid course-uuid]} update
        json (json/write-value-as-string (dissoc update :id :unit-id :course-id))]
    (db/insert! ds {:tname :exercise-changes
                    :data {:uuid uuid
                           :unit-uuid unit-uuid
                           :course-uuid course-uuid
                           :change-type change-type
                           :change-data json
                           :timestamp (util/get-utc-timestamp-string)}
                    :ret :1})))

(defn -add-unit-update! [ds {:keys [change-type update]}]
  (let [change-type (if (keyword? change-type) (name change-type) (str change-type))
        {:keys [uuid course-uuid]} update
        json (json/write-value-as-string (dissoc update :id :course-id :exercises))
        exercises (:exercises update)
        result (db/insert! ds {:tname :unit-changes
                               :data {:uuid uuid
                                      :course-uuid course-uuid
                                      :change-type change-type
                                      :change-data json
                                      :timestamp (util/get-utc-timestamp-string)}
                               :ret :1})]
    ; cascade exercises
    (->> (mapv (fn [exercise]
                 {:change-type change-type
                  :update (assoc
                           exercise
                           :course-uuid course-uuid
                           :unit-uuid uuid)})
               exercises)
         (mapv #(-add-exercise-update! ds %))
         (assoc result :exercises))))

(defn -add-course-update! [ds {:keys [change-type update]}]
  (let [change-type (if (keyword? change-type) (name change-type) (str change-type))
        uuid (:uuid update)
        json (json/write-value-as-string (dissoc update :id :units))
        units (:units update)
        result (db/insert! ds {:tname :course-changes
                               :data {:uuid uuid
                                      :change-type change-type
                                      :change-data json
                                      :timestamp (util/get-utc-timestamp-string)}
                               :ret :1})]
    ; cascade units
    (->> (mapv (fn [unit]
                 {:change-type change-type
                  :update (assoc unit :course-uuid uuid)}) units)
         (mapv #(-add-unit-update! ds %))
         (assoc result :units))))

(comment
  (require '[abantu.db.util :as db.util])
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
