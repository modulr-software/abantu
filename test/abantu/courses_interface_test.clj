(ns abantu.courses-interface-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
            [abantu.db.honey :as hon]
            [abantu.services.courses.interface :as sut]))

(def with-test-db tu/with-test-db)

(defn insert-course! [ds {:keys [user course]}]
  (when user
    (hon/insert! ds {:tname :users
                     :values user}))
  (when course
    (hon/insert! ds {:tname :courses
                     :values course})))

(defn read-fixture [file]
  (-> (io/resource (str "abantu/resources/courses/" file))
      (slurp)
      (json/read-value json/keyword-keys-object-mapper)))

(deftest test-query
  (testing "that querying one course that exists returns course with its units attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "lookup-by-id.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that querying one course by its uuid returns the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "lookup-by-uuid.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding all courses returns a vec of courses"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "all.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/all (sut/use-query ds))]
            (is (= (:output data) actual)))))))

  (testing "that finding courses by id returns the matching courses"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "find-by-id.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding courses by creator id returns the matching courses"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "find-by-creator-id.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that find with an id that doesn't exist returns an empty vec"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "find-missing.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual))))))))

(deftest test-mutation
  (testing "that successfully creating a course without units returns the created course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "create-course-basic.json")
              actual (sut/create (sut/use-mutation ds) (:input data))]
          (is (= (:output data) actual))))))

  (testing "that successfully creating a course with units returns the created course with its units attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "create-course-with-units.json")
              actual (sut/create (sut/use-mutation ds) (:input data))]
          (is (= (:output data) actual))))))

  (testing "that deleting a course removes it and its unit and exercise records from the db"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "delete-course.json")
              mut (sut/use-mutation ds)
              course-id (:id (:input data))]
          (run! (partial sut/create mut) (:seed data))
          (sut/delete mut (:input data))
          (is (not (hon/record? ds :courses course-id)))
          (is (empty? (hon/find ds {:tname :units
                                    :where [:= :course-id course-id]})))
          (is (empty? (hon/find ds {:tname :exercises
                                    :where [:= :course-id course-id]})))))))

  (testing "that setting the name updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-name.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-name (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the language updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-language.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-language (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the description updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-description.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-description (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting publishable updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-publishable.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-publishable (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting visible updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-visible.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-visible (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting review-pending updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-review-pending.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-review-pending (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual)))))))

  (testing "that setting the creator id updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-creator-id.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-creator-id (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))))))

(defn run-tests []
  (clojure.test/run-tests 'abantu.courses-interface-test))
