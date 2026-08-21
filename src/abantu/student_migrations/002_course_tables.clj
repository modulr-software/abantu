(ns abantu.student-migrations.002-course-tables
  (:require [abantu.db.student]
            [abantu.db.master]
            [abantu.db.tables :as tables]
            [abantu.db.util :as db.util]
            [abantu.db.honey :as hon]))

(defn run-up! [context]
  (let [ds-master (db.util/conn)
        ds-student (:ds-student context)
        id (:id context)

        courses (hon/find ds-master {:tname :courses
                                     :where [:= :creator-id id]})
        course-ids (mapv :id courses)

        units (hon/find ds-master {:tname :units
                                   :where [:in :course-id (or (seq course-ids) [0])]})
        unit-ids (mapv :id units)

        exercises (hon/find ds-master {:tname :exercises
                                       :where [:in :course-id (or (seq course-ids) [0])]})
        exercise-ids (mapv :id exercises)

        answers (hon/find ds-master {:tname :answers
                                     :where [:in :exercise-id (or (seq exercise-ids) [0])]})

        practice-sessions (hon/find ds-master {:tname :practice-sessions
                                               :where [:in :unit-id (or (seq unit-ids) [0])]})
        practice-sessions-ids (mapv :id practice-sessions)

        exercises-completed (hon/find ds-master {:tname :exercises-completed
                                                 :where [:in :practice-session-id (or (seq practice-sessions-ids) [0])]})

        comments (hon/find ds-master {:tname :comments
                                      :where [:in :course-id (or (seq course-ids) [0])]})]

    (tables/create-tables!
     ds-student
     :abantu.db.student
     [:courses
      :units
      :exercises
      :answers
      :practice-sessions
      :exercises-completed
      :comments
      :versions
      :course-changes
      :unit-changes
      :exercise-changes])

    (run!
     (hon/insert! ds-student {:tname :courses
                              :data (mapv #(dissoc % :creator-id) courses)})
     courses)

    (run!
     (hon/insert! ds-student {:tname :units
                              :data (mapv #(dissoc % :creator-id) units)})
     units)

    (run!
     (hon/insert! ds-student {:tname :exercises
                              :data exercises})

     exercises)

    (run!
     (hon/insert! ds-student {:tname :answers
                              :data answers})
     answers)

    (run!
     (hon/insert! ds-student {:tname :practice-sessions
                              :data practice-sessions})
     practice-sessions)

    (run!
     (hon/insert! ds-student {:tname :exercises-completed
                              :data exercises-completed})
     exercises-completed)

    (run!
     (hon/insert! ds-student {:tname :comments
                              :data comments})
     comments)))

(defn run-down! [context]
  (let [ds-student (:ds-student context)]
    (tables/drop-tables!
     ds-student
     [:courses
      :units
      :exercises
      :answers
      :practice-sessions
      :exercises-completed
      :comments])))

(comment
  ())
