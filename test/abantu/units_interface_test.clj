(ns abantu.units-interface-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
            [abantu.db.honey :as hon]
            [abantu.services.units.interface :as sut]))

(def with-test-db tu/with-test-db)

(defn insert-unit! [ds {:keys [unit]}]
  (hon/insert! ds {:tname :units
                   :values unit}))

(defn read-fixture [file]
  (-> (io/resource (str "abantu/resources/units/" file))
      (slurp)
      (json/read-value json/keyword-keys-object-mapper)))

(deftest test-query
  (testing "that querying one unit that exists returns unit with its metadata attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "lookup-by-id.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that querying one unit by its uuid returns unit with its metadata attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "lookup-by-uuid.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding all units returns a vec of units with their metadata attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "all.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/all (sut/use-query ds))]
            (is (= (:output data) actual)))))))

  (testing "that finding all units for a course returns a vec of units with their metadata attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "find-by-course.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that find with a course-id that doesn't exist returns an empty vec"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "find-missing.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual))))))))

(deftest test-mutation
  (testing "that successfully creating a unit without exercises returns the created unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "create-unit-basic.json")
              actual (sut/create (sut/use-mutation ds) (:input data))]
          (is (= (:output data) actual))))))

  (testing "that successfully creating a unit with exercises returns the created unit with its exercises attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "create-unit-with-exercises.json")
              actual (sut/create (sut/use-mutation ds) (:input data))]
          (is (= (:output data) actual))))))

  (testing "that deleting a unit removes it and its exercise records from the db"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "delete-unit.json")
              mut (sut/use-mutation ds)
              unit-id (:id (:input data))]
          (run! (partial sut/create mut) (:seed data))
          (sut/delete mut (:input data))
          (is (not (hon/record? ds :units unit-id)))
          (is (empty? (hon/find ds {:tname :exercises
                                    :where [:= :unit-id unit-id]}))))))

  (testing "that setting the name updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-name.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-name (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the description updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-description.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-description (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the level updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-level.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-level (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the type updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-type.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-type (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the course id updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-course-id.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-course-id (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the position updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-position.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-position (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))))

(defn run-tests []
  (clojure.test/run-tests 'abantu.units-interface-test))
