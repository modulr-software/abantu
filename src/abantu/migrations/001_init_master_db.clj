(ns abantu.migrations.001-init-master-db
  (:require [abantu.db.master]
            [abantu.db.honey :as db]
            [abantu.db.tables :as tables]
            [abantu.password :as password]
            [abantu.admins :as admins]
            [abantu.services.users :as users]))

(def user-types-seed
  [{:name "student"}
   {:name "creator"}
   {:name "admin"}])

(defn insert-admin! [ds {:keys [password] :as admin}]
  (prn "admin" admin)
  (db/insert! ds {:tname :users
                  :data (assoc admin
                               :password
                               (password/hash-password password))}))

(defn add-admin-type-id [id admin]
  (assoc admin :user-type-id id))

(defn run-up! [context]
  (let [ds-master (:db-master context)
        admins (admins/read)]

    (tables/create-tables!
     ds-master
     :abantu.db.master
     [:practice-sessions
      :exercises-completed
      :user-assigned-types
      :devices
      :user-types
      :users
      :courses
      :user-courses
      :units
      :exercises
      :answers])

    ;; seed the user types
    (db/insert! ds-master {:tname :user-types
                           :values user-types-seed})

    (let [admin-type-id (users/get-user-type-id ds-master "admin")]
      (run! (comp (partial insert-admin! ds-master)
                  (partial add-admin-type-id admin-type-id))
            admins))))

(defn run-down! [context]
  (let [ds-master (:db-master context)]
    (tables/drop-all-tables! ds-master)))

(comment

  ())
