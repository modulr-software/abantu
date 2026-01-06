(ns abantu.migrations.001-init-master-db
  (:require [abantu.admins :as admins]
            [abantu.db.master]
            [abantu.db.honey :as db]
            [abantu.db.tables :as tables]
            [abantu.password :as password]
            [abantu.services.users :as users]
            [clojure.data.json :as json]))

(def user-types-seed
  [{:name "student"}
   {:name "creator"}
   {:name "admin"}])

(defn insert-admin! [ds {:keys [password] :as admin}]
  (prn "admin" admin)
  (db/insert! ds {:tname :users
                  :values [(assoc admin
                                  :password
                                  (password/hash-password password))]}))

(defn add-admin-type-id [id admin]
  (assoc admin :user-type-id id))

(defn run-up! [context]
  (let [ds-master (:db-master context)
        admins (admins/read)]

    (tables/create-tables!
     ds-master
     :abantu.db.master
     [:user-assigned-types
      :user-types
      :users
      :vocab
      :courses
      :units
      :exercises
      :answers])

    ;; load in the vocab from dict.json into db
    (->>
     (-> (slurp "resources/dict.json")
         (json/read-str {:key-fn keyword}))
     (assoc {:tname :vocab
             :ret :*}
            :values)
     (db/insert! ds-master))

    ;; seed the user types
    (db/insert! ds-master {:tname :user-types
                           :values user-types-seed})

    ;; fetch the user type id for the admin role
    ;; assign it to the admins
    ;; save the admins to the users table
    (let [admin-type-id (users/get-user-type-id ds-master "admin")]
      (run! (comp (partial insert-admin! ds-master)
                  (partial add-admin-type-id admin-type-id))
            admins))))

(defn run-down! [context]
  (let [ds-master (:db-master context)]
    (tables/drop-all-tables! ds-master)))

(comment
  ())
