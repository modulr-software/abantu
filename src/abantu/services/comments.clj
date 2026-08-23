(ns abantu.services.comments
  (:require [abantu.db.interface :as db]
            [abantu.services.users :as users]))

(defn- append-user [ds {:keys [user-id] :as comment}]
  (-> (dissoc comment :user-id)
      (assoc :user (users/user ds user-id))))

(defn- append-resolved-by [ds {:keys [resolved-by] :as comment}]
  (if resolved-by
    (-> (dissoc comment :resolved-by)
        (assoc :resolved-by (users/user ds resolved-by)))
    comment))

(defn- append-users [ds comment]
  (->> comment
       (append-user ds)
       (append-resolved-by ds)))

(defn get-comment [ds id]
  (let [comment (db/find-one ds {:tname :comments
                                 :where [:= :id id]})]
    (when (some? comment)
      (append-users ds comment))))

(defn- resolved-where [type]
  (case type
    "resolved" [:= :resolved 1]
    "unresolved" [:= :resolved 0]
    nil))

(defn get-all
  ([ds] (get-all ds "all"))
  ([ds type]
   (let [where (resolved-where type)]
     (->> (db/find ds (cond-> {:tname :comments :ret :*}
                        where (assoc :where where)))
          (mapv (partial append-users ds))))))


(defn get-for-exercise
  ([ds exercise-id] (get-for-exercise ds exercise-id "all"))
  ([ds exercise-id type]
   (let [rwhere (resolved-where type)]
     (->> (db/find ds (cond-> {:tname :comments
                               :where [:= :exercise-id exercise-id]
                               :ret :*}
                        rwhere (assoc :where [:and [:= :exercise-id exercise-id] rwhere])))
          (mapv (partial append-users ds))))))

(defn save-comment! [ds {:keys [exercise-id unit-id course-id text user-id timestamp] :as comment}]
  (let [id (:id (db/insert! ds {:tname :comments
                                :data comment
                                :ret :1}))]
    (get-comment ds id)))

(defn resolve-comment! [ds id]
  (db/update! ds {:tname :comments
                  :where [:= :id id]
                  :data {:resolved 1}})
  (get-comment ds id))
