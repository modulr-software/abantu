(ns abantu.services.auth
  (:require [abantu.db.interface :as db]
            [abantu.middleware.auth.core :as mw]
            [abantu.services.users :as users]
            [abantu.config :as conf]
            [abantu.password :as password]
            [abantu.email.gmail :as gmail]
            [abantu.util :as util]))

(defn can-register-user? [ds {:keys [email password confirm-password] :as _user}]
  (cond
    (db/exists? ds {:tname :users
                    :where [:= :email email]})
    {:success false :error "User with this email alrady exists."}
    (not (= password confirm-password))
    {:success false :error "passwords do not match."}
    :else {:success true}))

(defn create-email-verification-url [hash]
  (str (conf/read-value :email :verification-link) "/" hash))

;; todo: send a email verification email
(defn register-noob! [ds user]
  (let [hash (util/uuid)
        user (users/create-user! ds (assoc user :email-hash hash))]
    (gmail/send-email {:to (:email user)
                       :subject "abantu email verification"
                       :body (str "Please go to the below link to verify your email:\n\n"
                                  (create-email-verification-url hash))
                       :type :text/plain})
    (merge {:user user}
           (mw/create-session user))))

(defn login-user [ds {:keys [email password]}]
  (let [{:keys [id] :as user} (db/find-one ds {:tname :users
                                               :where [:= :email email]})
        password-match? (password/verify-password password (:password user))
        user (users/get-user ds id)]
    (if password-match?
      {:success true :data (mw/create-session user)}
      {:success false :error "Incorrect username or password"})))

(defn verify-email [ds email-hash]
  (let [{:keys [id] :as user} (users/remove-user-email-hash! ds email-hash)]
    (when user
      (db/update! ds {:tname :users
                      :data {:email-verified true}
                      :where [:= :id id]
                      :ret :1}))))

(comment

  (def ds (db/ds :master))
  (register-noob! ds {:email "kaidan13th@gmail.com"
                      :role "student"
                      :password "micro"})

  (users/get-all-users ds)
  ())
