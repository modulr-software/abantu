(ns abantu.exercises-interface-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
            [abantu.db.honey :as hon]
            [abantu.db.util :as db.util]
            [abantu.services.changes.interface :as changes]
            [abantu.services.exercises.interface :as sut]))

(def master-test-id 1)
(def student-test-id 2)

(defn- seed-course-and-units! [ds]
  (hon/insert! ds {:tname :courses
                   :values {:id 1
                            :uuid "test-course-uuid"}})
  (run! (fn [[id uuid]]
          (hon/insert! ds {:tname :units
                           :values {:id id
                                    :uuid uuid
                                    :name (str "Unit " id)
                                    :course-id 1}}))
        {1 "test-unit-uuid-1"
         2 "test-unit-uuid-2"}))

(defn with-test-student-db [f]
  (tu/with-test-student-db master-test-id student-test-id
    (fn [ds]
      (seed-course-and-units! ds)
      ;; exercises resolve comment authors from the master db, so point
      ;; db/ds at the test master db (same trick as create-test-student-db!)
      (with-open [master-ds (db.util/conn :test master-test-id)]
        (with-redefs [db.util/conn (fn [& _] master-ds)]
          (f ds))))))

(defn insert-exercise! [ds {:keys [exercise answers comments exercises-completed]}]
  (hon/insert! ds {:tname :exercises
                   :values exercise})
  (when (seq answers)
    (hon/insert! ds {:tname :answers
                     :values answers}))
  (when (seq comments)
    (hon/insert! ds {:tname :comments
                     :values comments}))
  (when (seq exercises-completed)
    (hon/insert! ds {:tname :exercises-completed
                     :values exercises-completed})))

(defn read-fixture [file]
  (-> (io/resource (str "abantu/resources/exercises/" file))
      (slurp)
      (json/read-value json/keyword-keys-object-mapper)))

(deftest test-query
  (testing "that querying one exercise that exists returns exercise with its metadata attached"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-by-id.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that querying an exercise without answers returns an exercise with an empty answers vec"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-no-answers.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that querying one exercise that doesn't exist returns nil"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-missing.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding all exercises returns a vec of exercises with their metadata attached"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "all.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/all (sut/use-query ds))]
            (is (= (:output data) actual)))))))

  (testing "that querying one exercise by its uuid returns exercise with its metadata attached"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-by-uuid.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding all exercises for a course returns a vec of exercises with their metadata attached"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-by-course.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding all exercises for a unit returns a vec of exercises with their metadata attached"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-by-unit.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding exercises for a unit and course returns the matching exercises with their metadata attached"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-by-unit-course.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that find with an id that doesn't exist returns an empty vec"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-missing.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual))))))))

(deftest test-mutation
  (testing "that successfully creating an exercise returns the created exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "create-bubbles-basic.json")
              actual (sut/create (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
          (is (= (:output data) actual))))))

  (testing "that failing to create an exercise throws"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "create-invalid.json")]
          (is (thrown? clojure.lang.ExceptionInfo
                       (sut/create (sut/use-mutation ds (changes/use-mutation ds)) (:input data))))))))

  (testing "that deleting an exercise removes it and its comment and answer records"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "delete-exercise.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (sut/delete (sut/use-mutation ds (changes/use-mutation ds)) (:input data))
          (let [actual (into {} (map (fn [t] [t (hon/find ds {:tname t :ret :*})]))
                             (keys (:output data)))]
            (is (= (:output data) actual)))))))

  (testing "that setting the unit updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-unit.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-unit (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the instruction updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-instruction.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-instruction (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the question content updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-question-content.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-question-content (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the answer type updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-answer-type.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-answer-type (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the level updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-level.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-level (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the correct message updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-correct-message.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-correct-message (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the incorrect message updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-incorrect-message.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-incorrect-message (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the position updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-position.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-position (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the options updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-options.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-options (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the answers updates the exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-answers.json")]
          (run! (partial insert-exercise! ds) (:seed data))
          (let [actual (sut/set-answers (sut/use-mutation ds (changes/use-mutation ds)) (:input data))]
            (is (= (:output data) actual))))))))

(defn run-tests []
  (clojure.test/run-tests 'abantu.exercises-interface-test))
