(ns abantu.changes-migrate-test
  (:require [clojure.test :as t]
            [abantu.db.util :as db.util]
            [abantu.db.tables :as tables]
            [abantu.db.student]
            [abantu.db.honey :as hon]
            [clojure.java.io :as io]
            [abantu.config :as conf]
            [abantu.services.changes.interface :as changes-intf]))

(defn tmp-dir []
  (let [f (java.io.File/createTempFile "abantu-test-" "")]
    (.delete f)
    (.mkdir f)
    (.getAbsolutePath f)))

(defn cleanup-test-db!
  "Remove test DB files for `test-id` and delete `tmp-dir` if present.
  Safe to call from tests' finally blocks; errors are ignored."
  [tmp-dir test-id]
  (try (db.util/remove-db-files! :test test-id) (catch Throwable _))
  (try (db.util/remove-db-files!) (catch Throwable _))
  (try (when (and tmp-dir (.exists (io/file tmp-dir)))
         (io/delete-file (io/file tmp-dir) true)) (catch Throwable _))
  nil)

(t/deftest migrate-setup-test
  (t/testing "create test master + student DBs and add a version and updates"
    (let [tmp (tmp-dir)
          orig-read (var-get #'conf/read-value)]
      (with-redefs [conf/read-value (fn [& ks]
                                      (if (and (= (first ks) :database)
                                               (= (second ks) :dir))
                                        tmp
                                        (apply orig-read ks)))]

        ;; prepare master DB using student schema (contains uuid columns)
        (let [master-ds (db.util/conn)]
          (tables/create-tables! master-ds :abantu.db.student
                                 [:courses :units :exercises :answers
                                  :practice-sessions :exercises-completed :comments])

          ;; create fresh test student DB by copying master schema
          (db.util/create-test-db! 1)

          (let [ds-student (db.util/conn :test 1)]
            (tables/create-tables! ds-student :abantu.db.student
                                   [:courses :units :exercises :answers :practice-sessions
                                    :exercises-completed :comments :versions :course-changes
                                    :unit-changes :exercise-changes])

            ;; create version and changes
            (let [vcm (changes-intf/use-mutation ds-student)
                  version (changes-intf/add-version! vcm {:course-id 1})]

              (changes-intf/add-course-update! vcm {:version-id (:id version)
                                                    :change-type "create"
                                                    :update {:uuid "test-course-uuid"
                                                             :name "Test Course"
                                                             :language "test"
                                                             :description "desc"}})

              (changes-intf/add-unit-update! vcm {:version-id (:id version)
                                                  :change-type "create"
                                                  :update {:uuid "test-unit-uuid"
                                                           :course-uuid "test-course-uuid"
                                                           :name "Test Unit"
                                                           :description "unit desc"
                                                           :type "lesson"
                                                           :exercises []}})

              (changes-intf/add-exercise-update! vcm {:version-id (:id version)
                                                      :change-type "create"
                                                      :update {:uuid "test-ex-uuid"
                                                               :unit-uuid "test-unit-uuid"
                                                               :course-uuid "test-course-uuid"
                                                               :instruction "Translate the following"
                                                               :question-content "who are you"
                                                               :answer-type "bubbles"
                                                               :options ["wat" "wie"]
                                                               :correct-message "correct!"
                                                               :incorrect-message "nope"
                                                               :answers [["wie"]]}})

              ;; debug + run migration
              (let [vcq (changes-intf/use-query ds-student)
                    course-changes (changes-intf/find-course-changes vcq {:version-id (:id version)})]
                (prn "course-changes:" course-changes)
                (t/is (seq course-changes))

                (changes-intf/migrate-up! vcm {:id (:id version)
                                               :timestamp (:timestamp version)})

                ;; verify master DB
                (let [master (db.util/conn)
                      course (hon/find master {:tname :courses
                                               :where [:= :uuid "test-course-uuid"]
                                               :ret :1})
                      unit (hon/find master {:tname :units
                                             :where [:= :uuid "test-unit-uuid"]
                                             :ret :1})
                      exercise (hon/find master {:tname :exercises
                                                 :where [:= :uuid "test-ex-uuid"]
                                                 :ret :1})]
                  (t/is (some? version))
                  (t/is (some? course))
                  (t/is (= (:name course) "Test Course"))
                  (t/is (some? unit))
                  (t/is (= (:name unit) "Test Unit"))
                  (t/is (some? exercise))
                  (t/is (= (:instruction exercise) "Translate the following"))))))))

      (cleanup-test-db! tmp 1))))

(defn run-tests []
  (t/run-tests 'source-be.changes-migrate-test))
