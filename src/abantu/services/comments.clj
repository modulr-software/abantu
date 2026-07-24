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

(defn get-all [ds]
  (->> (db/find ds {:tname :comments :ret :*})
       (mapv (partial append-users ds))))

(defn get-for-exercise [ds exercise-id]
  (->> (db/find ds {:tname :comments
                    :where [:= :exercise-id exercise-id]
                    :ret :*})
       (mapv (partial append-users ds))))

(defn save-comment! [ds {:keys [exercise-id unit-id course-id text user-id timestamp] :as comment}]
  (db/insert! ds {:tname :comments
                  :data comment
                  :ret :1}))

(defn resolve-comment! [ds id]
  (db/update! ds {:tname :comments
                  :where [:= :id id]
                  :data {:resolved 1}})
  (db/find-one ds {:tname :comments
                   :where [:= :id id]}))
