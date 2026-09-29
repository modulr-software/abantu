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
  ([ds type] (get-all ds type nil))
  ([ds type users-ds-fn]
   (let [where (resolved-where type)
         resolve-users (or users-ds-fn (constantly ds))
         comments (db/find ds (cond-> {:tname :comments :ret :*}
                                where (assoc :where where)))]
     (if (seq comments)
       (mapv (partial append-users (resolve-users)) comments)
       []))))

(defn get-for-exercise
  "Comments live alongside the exercise, but their authors live in the master
   db. Pass `users-ds-fn` (a 0-arity fn returning the ds to resolve authors
   from) when that differs from `ds`; it is only called when the exercise
   actually has comments."
  ([ds exercise-id] (get-for-exercise ds exercise-id "all" nil))
  ([ds exercise-id type] (get-for-exercise ds exercise-id type nil))
  ([ds exercise-id type users-ds-fn]
   (let [rwhere (resolved-where type)
         resolve-users (or users-ds-fn (constantly ds))
         comments (db/find ds (cond-> {:tname :comments
                                       :where [:= :exercise-id exercise-id]
                                       :ret :*}
                                rwhere (assoc :where [:and [:= :exercise-id exercise-id] rwhere])))]
     (if (seq comments)
       (mapv (partial append-users (resolve-users)) comments)
       []))))

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
