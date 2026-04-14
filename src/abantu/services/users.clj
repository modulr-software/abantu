(ns abantu.services.users
  (:require [abantu.db.interface :as db]
            [abantu.util :as util]
            [abantu.password :as password]))

(defn- process-bools [user]
  (util/parse-bool-keys user [:email-verified]))


(defn get-user-role [ds user-type-id]
  (->> (db/find ds {:tname :user-types
                    :where [:= :id user-type-id]
                    :ret :1})
       (:name)))

(defn- process-role [ds {:keys [user-type-id] :as user}]
  (-> (dissoc user :user-type-id)
      (assoc :role (get-user-role ds user-type-id))))

(defn get-user [ds id]
  (->> (db/find-one ds {:tname :users
                        :where [:= :id id]})
       (process-bools)
       (process-role ds)))

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
         (mapv (comp #(dissoc % :password)
                     process-bools
                     (partial process-role ds))))))

(defn get-all-users [ds]
  (->> (db/find ds {:tname :users
                    :ret :*})
       (mapv (comp #(dissoc % :password)
                   process-bools
                   (partial process-role ds)))))

(defn create-user! [ds {:keys [role password] :as user}]
  (let [{:keys [id]} (->> (-> (dissoc user :role :password)
                              (assoc :user-type-id (get-user-type-id ds role)
                                     :password (password/hash-password password)))
                          (assoc {:tname :users :ret :1} :values)
                          (db/insert! ds))]
    (get-user ds id)))

(defn get-user-by-email-hash [ds hash]
  (let [user (db/find-one ds {:tname :users
                              :where [:= :email-hash hash]
                              :ret :1})]
    user))

(defn remove-user-email-hash! [ds hash]
  (let [{:keys [id] :as user} (get-user-by-email-hash ds hash)]
    (when user
      (db/update! ds {:tname :users
                      :values {:email-hash nil}
                      :where [:= :id id]})
      (dissoc user :email-hash))))

(comment
  (def ds (db/ds :master))

  (get-user-type-id ds "admin")
  (get-all-admins ds)
  (get-all-users ds)
  (get-user-types ds)

  (get-user ds 1)

  (create-user! ds {:email "keagan@nonce.com"
                    :role "admin"
                    :password "hellothere"})

  (db/delete! ds {:tname :users})

  (get-user ds 10)
  (get-all-users ds)

  ())
