(ns abantu.services.users
  (:require [abantu.db.interface :as db]
            [abantu.util :as util]
            [abantu.password :as password]
            [abantu.email.gmail :as gmail]
            [abantu.config :as conf]))


(defn- process-bools [user]
  (util/parse-bool-keys user [:onboarded :email-verified]))

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

(defn can-register-user? [ds {:keys [email password confirm-password] :as _user}]
  (cond
    (db/exists? ds {:tname :users
                    :where [:= :email email]})
    {:success false :error "User with this email alrady exists."}
    (not (= password confirm-password))
    {:success false :error "passwords do not match."}
    :else {:success true}))

(defn get-user-by-email-hash [ds hash]
  (let [user (db/find-one ds {:tname :users
                              :where [:= :email-hash hash]
                              :ret :1})]
    user))

(defn remove-user-hash! [ds hash]
  (let [{:keys [id] :as user} (get-user-by-email-hash ds hash)]
    (when user
      (db/update! ds {:tname :users
                      :values {:hash nil}
                      :where [:= :id id]})
      (dissoc user :hash))))

(defn create-email-verification-url [hash]
  (str (conf/read-value :email :verification-link) "/" hash))

;; todo: send a email verification email
(defn register-noob! [ds user]
  (let [hash (util/uuid)]
  (create-user! ds user)
  (gmail/send-email {:to "kaidan13th@gmail.com"
                     :subject "abantu email verification"
                     :body (str "Please click on the below link to verify your email:\n\n"
                                (create-email-verification-url hash))
                     :type :text/plain})))

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
  
  (register-noob! ds {:email "kaidan13th@gmail.com"
                      :role "student"
                      :password "micro"})
  
  (db/delete! ds {:tname :users
                  :where [:= :email "kaidan13th@gmail.com"]})
  
  (get-user ds 10)
  (get-all-users ds)
  
  ()
  )