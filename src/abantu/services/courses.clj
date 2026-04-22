(ns abantu.services.courses
  (:require [abantu.db.interface :as db]
            [abantu.services.units :as units]
            [abantu.services.users :as users]))

(defn- append-units [ds {:keys [id] :as course}]
  (let [units (units/get-units ds id)]
    (assoc course :units (mapv #(dissoc % :creator-id) (or units [])))))

(defn- append-creator [ds {:keys [creator-id] :as course}]
  (if creator-id
    (let [creator (users/get-user ds creator-id)]
      (-> (assoc course :creator (or creator nil))
          (dissoc :creator-id)))
    (-> (assoc course :creator nil)
        (dissoc :creator-id))))

(defn get-all [ds]
  (let [courses (db/find ds {:tname :courses
                             :ret :*})]
    (mapv (comp (partial append-units ds)
                (partial append-creator ds)) courses)))

(defn get-course [ds id]
  (let [course (db/find ds {:tname :courses
                            :where [:= :id id]
                            :ret :1})]
    (when (some? course)
      (->> (append-units ds course)
           (append-creator ds)))))

(defn save-course! [ds {:keys [units] :as course}]
  (let [{:keys [id] :as result} (db/insert! ds {:tname :courses
                                                :data [(dissoc course :units)]
                                                :ret :1})
        units (when (seq units)
                (->> (mapv #(assoc % :course-id id) units)
                     (units/save-units! ds)))
        result' (append-creator ds result)]
    (assoc result' :units (or units []))))

(defn save-courses! [ds courses]
  (mapv (partial save-course! ds) courses))

(defn update-course! [ds {:keys [id] :as course}]
  (let [exists? (db/exists? ds {:tname :courses
                                :where [:= :id id]})]
    (when exists?
      (db/update! ds {:tname :courses
                      :data (dissoc course :id)
                      :where [:= :id id]}))
    exists?))

(defn delete-course! [ds id]
  (let [{:keys [units] :as course} (get-course ds id)]
    (when (seq units)
      (run! (comp (partial units/delete-unit! ds)
                  :id)
            units))
    (when (some? course)
      (db/delete! ds {:tname :courses
                      :where [:= :id id]}))))

(defn assign-course-to-user! [ds user-id course-id]
  (let [exists? (db/exists? ds {:tname :user-courses
                                :where [:and
                                        [:= :user-id user-id]
                                        [:= :course-id course-id]]})]
    (when (not exists?)
      (db/insert! ds {:tname :user-courses
                      :data {:user-id user-id
                             :course-id course-id}
                      :ret :1}))))


(defn courses-by-user [ds user-id]
  (let [course-ids (mapv :course-id (db/find ds {:tname :user-courses
                                                 :where [:= :user-id user-id]
                                                 :ret :*}))
        courses (db/find ds {:tname :courses
                             :where [:in :id course-ids]
                             :ret :*})]
    (mapv (comp (partial append-units ds)
                (partial append-creator ds)) courses)))


(defn course-by-user [ds user-id course-id]
  (when (db/find-one ds {:tname :user-courses
                           :where [:and
                                   [:= :user-id user-id]
                                   [:= :course-id course-id]]})
    (get-course ds course-id)))

(defn exercises-for-course [ds id]
  (db/find ds {:tname :exercises
               :where [:= :course-id id]
               :ret :*}))

(defn used-instructions [ds id]
    (->> (exercises-for-course ds id) 
         (mapv :instruction)
         set))


(defn remove-course-from-user! [ds user-id course-id]
  (db/delete! ds {:tname :user-courses
                  :where [:and
                          [:= :user-id user-id]
                          [:= :course-id course-id]]
                  :ret :1})
  (not (course-by-user ds user-id course-id)))

(comment
  (def ds (db/ds :master))

  (require '[honey.sql.helpers :as hsql])
  (-> (hsql/where {:where [:= :user-id 1]} := :course-id 1)
      )
  

  ;; create a course with or without units = pass
  (let [email "merveillevaneck@gmail.com"
        user-id (->> (db/find-one ds {:tname :users
                                      :where [:= :email email]})
                     (:id))
        units []]
    (prn "user-id" user-id)
    (save-course! ds {:name "the best course ever again"
                      :language "xhosa"
                      :status "in-progress"
                      :units units
                      :creator-id user-id}))

  ;; insert course with a unit = pass
  (let [email "merveillevaneck@gmail.com"
        user-id (->> (db/find-one ds {:tname :users
                                      :where [:= :email email]})
                     (:id))
        units [{:name "some unit 2"
                :description "some description 2"
                :exercises [{:question-type "translation"
                             :question "how are you?"
                             :options ["u-" "-njani" "-phi" "wena"]
                             :answers [["u-" "-njani"]]}]
                :level 1}]]
    (save-course! ds {:name "some course 2"
                      :language "xhosa"
                      :status "in-progress"
                      :units units
                      :creator-id user-id}))

  ;;delete a course = pass
  (delete-course! ds 16)
  (get-all ds)


  (db/find ds {:tname :courses})
  (abantu.db.honey/execute! ds {:select [:*]
                                :from :courses})

  ;;update a course = pass
  (update-course! ds {:id 1
                      :name "some unit whatever"})

  (assign-course-to-user! ds 1 1)
  (remove-course-from-user! ds 1 1)
  (courses-by-user ds 1)
  (users/get-user ds 1)
  (course-by-user ds 1 1)

  (used-instructions ds 1)

  ()
  )
