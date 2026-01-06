(ns abantu.services.users
  (:require [abantu.db.interface :as db]))

(defn get-user [ds id]
  (-> (db/find-one ds {:tname :users
                   :where [:= :id id]})
      (dissoc :password)))

(defn get-user-type-id [ds role]
  (->> (db/find ds {:tname :user-types
                    :where [:= :name role]
                    :ret :1})
       (:id)))

(defn get-user-types [ds]
  (db/find ds {:tname :user-types
               :ret :*}))

(defn get-all-admins [ds]
  (let [id (get-user-type-id ds "admin")]
    (->> (db/find ds {:tname :users
                 :where [:= :user-type-id id]
                 :ret :*})
         (mapv #(dissoc % :password)))))

(defn get-all-users [ds]
  (->> (db/find ds {:tname :users
               :ret :*})
       (mapv #(dissoc % :password)))
  )


(comment
  (def ds (db/ds :master))

  (get-user-type-id ds "admin")
  (get-all-admins ds)
  (get-all-users ds)
  (get-user-types ds)
  ())