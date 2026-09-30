(ns abantu.changes-migrate-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
            [abantu.db.honey :as hon]
            [abantu.services.courses.interface :as course]
            [abantu.services.changes.interface :as sut]
            [abantu.services.versions.interface :as version]))

(def with-test-dbs tu/with-test-dbs)

(defn insert-seed! [ds {:keys [version course-change unit-change exercise-change]}]
  (when version
    (hon/insert! ds {:tname :versions
                     :values version}))
  (when course-change
    (hon/insert! ds {:tname :course-changes
                     :values course-change}))
  (when unit-change
    (hon/insert! ds {:tname :unit-changes
                     :values unit-change}))
  (when exercise-change
    (hon/insert! ds {:tname :exercise-changes
                     :values exercise-change})))

(defn read-fixture [file]
  (-> (io/resource (str "abantu/resources/migrate/" file))
      (slurp)
      (json/read-value json/keyword-keys-object-mapper)))

(deftest test-migration
  (testing "that migrating up on a new course that has not been published
            yet successfully copies all course data to master"
    (with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "migrate-new-course.json")]
          (run! (partial insert-seed! student-ds) (:seed data))
          (let [version (version/lookup (version/use-query student-ds) (:query data))]
            (sut/migrate-up! (sut/use-mutation student-ds) version)
            (is (= (:output data)
                   (course/all (course/use-query master-ds)))))))))

  (testing "that migrating up on a course that already exists with update
            changes successfully applies updates to master"
    (with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "migrate-update-course.json")]
          (course/create (course/use-mutation master-ds) (:master data))
          (run! (partial insert-seed! student-ds) (:seed data))
          (let [version (version/lookup (version/use-query student-ds) (:query data))]
            (sut/migrate-up! (sut/use-mutation student-ds) version)
            (is (= (:output data)
                   (course/all (course/use-query master-ds)))))))))

  (testing "that migrating up on an existing course with deletion updates
            for units and exercises successfully deletes the specified units
            and exercises"
    (with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "migrate-delete-unit-exercise.json")]
          (course/create (course/use-mutation master-ds) (:master data))
          (run! (partial insert-seed! student-ds) (:seed data))
          (let [version (version/lookup (version/use-query student-ds) (:query data))]
            (sut/migrate-up! (sut/use-mutation student-ds) version)
            (is (= (:output data)
                   (course/all (course/use-query master-ds)))))))))

  (testing "that migrating up on an existing course to and applying changes
            to move an exercise to a different unit correctly sets the unit
            id on master"
    (with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "migrate-move-exercise.json")]
          (course/create (course/use-mutation master-ds) (:master data))
          (run! (partial insert-seed! student-ds) (:seed data))
          (let [version (version/lookup (version/use-query student-ds) (:query data))]
            (sut/migrate-up! (sut/use-mutation student-ds) version)
            (is (= (:output data)
                   (course/all (course/use-query master-ds)))))))))

  (testing "that migrating up on an existing course with changes that delete
            the course and all its units and exercises then successfully
            deletes all associated records on master"
    (with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "migrate-delete-course.json")]
          (course/create (course/use-mutation master-ds) (:master data))
          (run! (partial insert-seed! student-ds) (:seed data))
          (let [version (version/lookup (version/use-query student-ds) (:query data))]
            (sut/migrate-up! (sut/use-mutation student-ds) version)
            (is (= (:output data)
                   (course/all (course/use-query master-ds)))))))))

  (testing "that migrating up an existing course with 2 new units and 4 new exercises, moving 2 existing exercises to one of the new units, assigning the 4 new exercises to the 2 new units each, and then deleting one of the new units again afterwards - results in 1 new unit being in the database and the 2 existing exercises are moved successfully to that unit."
    (with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "migrate-mixed-course-changes.json")]
          (course/create (course/use-mutation master-ds) (:master data))
          (run! (partial insert-seed! student-ds) (:seed data))
          (let [version (version/lookup (version/use-query student-ds) (:query data))]
            (sut/migrate-up! (sut/use-mutation student-ds) version)
            (is (= (:output data)
                   (course/all (course/use-query master-ds))))))))))

(defn run-tests []
  (clojure.test/run-tests 'abantu.changes-migrate-test))
