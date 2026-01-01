(ns abantu.migrations.001-init-master-db
  (:require [abantu.admins :as admins]
            [abantu.db.master]
            [abantu.db.honey :as db]
            [abantu.db.tables :as tables]
            [abantu.password :as password]
            [clojure.data.json :as json]))

(def user-types-seed
  [{:name "student"}
   {:name "creator"}
   {:name "admin"}])

(defn insert-admin! [ds {:keys [password] :as admin}]
  (db/insert! ds {:tname :users
                  :values [(assoc admin
                                  :password
                                  (password/hash-password password))]}))

(defn run-up! [context]
  (let [ds-master (:db-master context)
        admins (admins/read)]

    (tables/create-tables!
     ds-master
     :abantu.db.master
     [:user-assigned-types :user-types :users :vocab :units :exercises :answers])

    (->>
     (-> (slurp "resources/dict.json")
         (json/read-str {:key-fn keyword}))
     (assoc {:tname :vocab
             :ret :*}
            :values)
     (db/insert! ds-master))

    (db/insert! ds-master {:tname :user-types
                           :values user-types-seed})

    (run! (partial insert-admin! ds-master) admins)))

(defn run-down! [context]
  (let [ds-master (:db-master context)]
    (tables/drop-all-tables! ds-master)))

(comment
  (admins/read)
  (tables/create-tables!
   (abantu.db.util/conn :master)
   :abantu.db.master
   [:users :user-types :user-assigned-types :vocab :units :exercises :answers])

  (admins/read)

  (db/insert! (abantu.db.util/conn :master)
              {:email "merveillevaneck@gmail.com"
               :password (password/hash-password "M3rveille")})

  (run! (partial insert-admin! (abantu.db.util/conn :master)) (admins/read))
  ())
