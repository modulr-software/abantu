(ns abantu.courses-interface-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
            [abantu.db.honey :as hon]
            [abantu.services.changes.interface :as changes]
            [abantu.services.courses.interface :as sut]))

(def with-test-db tu/with-test-db)

(def master-test-id 1)
(def student-test-id 2)

;; the change feed tables only exist on the student db, so any test that
;; exercises change recording has to run against the student db
(defn with-test-student-db [f]
  (tu/with-test-student-db master-test-id student-test-id
    (fn [ds]
      (f ds))))

(defn- changes [ds tname]
  (->> (hon/find ds {:tname tname :ret :*})
       (mapv #(update % :change-data json/read-value json/keyword-keys-object-mapper))
       (sort-by :id)))

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
          (is (= (:output data) actual))))  ))
  (testing "that successfully creating a course with units returns the created course with its units attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "create-course-with-units.json")
              actual (sut/create (sut/use-mutation ds) (:input data))]
          (is (= (:output data) actual))))  ))
  (testing "that creating a course cascades changes for the course, every unit and every exercise"
    (with-test-student-db
      (fn [ds]
        ;; delete-course's seed is a full course -> unit -> exercise tree and is
        ;; a valid create input
        (let [input (first (:seed (read-fixture "delete-course.json")))
              {:keys [uuid units]} input
              expected-exercises (mapcat :exercises units)]
          (sut/create (sut/use-mutation ds {:changes-api (changes/use-mutation ds)}) input)
          (let [courses (changes ds :course-changes)
                units' (changes ds :unit-changes)
                exercises' (changes ds :exercise-changes)]
            (is (= 1 (count courses)))
            (is (= "create" (:change-type (first courses))))
            (is (= {:uuid uuid
                    :name "Course One"
                    :language "english"}
                   (select-keys (:change-data (first courses)) [:uuid :name :language])))
            ;; the unit tree is not inlined into the course change payload
            (is (nil? (:units (:change-data (first courses)))))
            (is (= (count units) (count units')))
            (is (= (mapv :uuid units) (mapv #(get-in % [:change-data :uuid]) units')))
            (is (every? #(= uuid (get-in % [:change-data :course-uuid])) units'))
            (is (= (count expected-exercises) (count exercises')))
            (is (every? #(= "create" (:change-type %)) exercises'))
            (is (= (mapv :uuid expected-exercises)
                   (mapv #(get-in % [:change-data :uuid]) exercises')))
            (is (every? #(= uuid (get-in % [:change-data :course-uuid])) exercises'))
            (is (= (set (map :uuid units))
                   (set (map #(get-in % [:change-data :unit-uuid]) exercises'))))
            ;; comments are never replayable, so they must not reach the feed
            (is (every? #(not (contains? (:change-data %) :comments)) exercises')))))  ))

  (testing "that creating a course without a changes api records nothing"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "create-course-with-units.json")]
          (sut/create (sut/use-mutation ds) (:input data))
          (is (empty? (changes ds :course-changes)))
          (is (empty? (changes ds :unit-changes)))
          (is (empty? (changes ds :exercise-changes))))  )))
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
                                    :where [:= :course-id course-id]}))))  )))
  (testing "that deleting a course cascades changes for the course, every unit and every exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "delete-course.json")
              mut (sut/use-mutation ds {:changes-api (changes/use-mutation ds)})
              seed (first (:seed data))
              expected-exercises (mapcat :exercises (:units seed))]
          ;; seed without a changes api so only the delete lands in the feed
          (sut/create (sut/use-mutation ds) seed)
          (sut/delete mut (:input data))
          (let [courses (changes ds :course-changes)
                units' (changes ds :unit-changes)
                exercises' (changes ds :exercise-changes)]
            (is (= 1 (count courses)))
            (is (= "delete" (:change-type (first courses))))
            ;; a delete change only carries identity, never the full record
            (is (= #{:uuid} (set (keys (:change-data (first courses))))))
            (is (= (mapv :uuid (:units seed)) (mapv #(get-in % [:change-data :uuid]) units')))
            (is (every? #(= (:uuid seed) (get-in % [:change-data :course-uuid])) units'))
            (is (= (count expected-exercises) (count exercises')))
            (is (= (mapv :uuid expected-exercises)
                   (mapv #(get-in % [:change-data :uuid]) exercises')))
            (is (every? #(= "delete" (:change-type %)) exercises'))
            (is (every? #(= (:uuid seed) (get-in % [:change-data :course-uuid])) exercises'))
            (is (= (set (map :uuid (:units seed)))
                   (set (map #(get-in % [:change-data :unit-uuid]) exercises'))))))  )))

  (testing "that setting a course field records a change for the course"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-name.json")
              mut (sut/use-mutation ds {:changes-api (changes/use-mutation ds)})
              course (:course (first (:seed data)))]
          (run! (partial insert-course! ds) (:seed data))
          (sut/set-name mut {:id (:id course) :name "renamed"})
          (let [[change] (changes ds :course-changes)]
            (is (= "set-name" (:change-type change)))
            ;; :id is stripped from the change payload
            (is (= {:name "renamed" :uuid (:uuid course)} (:change-data change))))))  ))
  (testing "that setting the name updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-name.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-name (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the language updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-language.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-language (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the description updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-description.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-description (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the publishable updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-publishable.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-publishable (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the visible updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-visible.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-visible (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the review pending updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-review-pending.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-review-pending (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the creator id updates the course"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-creator-id.json")]
          (run! (partial insert-course! ds) (:seed data))
          (let [actual (sut/set-creator-id (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
)

(defn run-tests []
  (clojure.test/run-tests 'abantu.courses-interface-test))
