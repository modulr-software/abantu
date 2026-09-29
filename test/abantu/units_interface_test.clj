(ns abantu.units-interface-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
            [abantu.db.honey :as hon]
            [abantu.services.changes.interface :as changes]
            [abantu.services.units.interface :as sut]))

(def with-test-db tu/with-test-db)

(def master-test-id 1)
(def student-test-id 2)

;; the change feed tables only exist on the student db, so any test that
;; exercises change recording has to run against the student db
(defn with-test-student-db [f]
  (tu/with-test-student-db master-test-id student-test-id
    (fn [ds]
      (run! (fn [course] (hon/insert! ds {:tname :courses :values course}))
            [{:id 1 :uuid "test-course-uuid"}
             {:id 2 :uuid "test-course-2-uuid"}])
      (f ds))))

(defn- changes [ds tname]
  (->> (hon/find ds {:tname tname :ret :*})
       (mapv #(update % :change-data json/read-value json/keyword-keys-object-mapper))
       (sort-by :id)))

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
          (is (= (:output data) actual))))  ))
  (testing "that successfully creating a unit with exercises returns the created unit with its exercises attached"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "create-unit-with-exercises.json")
              actual (sut/create (sut/use-mutation ds) (:input data))]
          (is (= (:output data) actual))))  ))
  (testing "that creating a unit with exercises cascades changes for the unit and each exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "create-unit-with-exercises.json")
              {:keys [uuid exercises]} (:input data)]
          (sut/create (sut/use-mutation ds (changes/use-mutation ds)) (:input data))
          (let [units (changes ds :unit-changes)
                exercises' (changes ds :exercise-changes)]
            (is (= 1 (count units)))
            (is (= "create" (:change-type (first units))))
            (is (= {:uuid uuid
                    :course-uuid "test-course-uuid"
                    :name "Unit One"
                    :type "lesson"}
                   (select-keys (:change-data (first units)) [:uuid :course-uuid :name :type])))
            ;; the exercise tree is not inlined into the unit change payload
            (is (nil? (:exercises (:change-data (first units)))))
            (is (= (count exercises) (count exercises')))
            (is (every? #(= "create" (:change-type %)) exercises'))
            (is (= (mapv :uuid exercises)
                   (mapv #(get-in % [:change-data :uuid]) exercises')))
            (is (every? #(= uuid (get-in % [:change-data :unit-uuid])) exercises'))
            ;; comments are never replayable, so they must not reach the feed
            (is (every? #(not (contains? (:change-data %) :comments)) exercises'))))  )))
  (testing "that creating a unit without a changes api records nothing"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "create-unit-with-exercises.json")]
          (sut/create (sut/use-mutation ds) (:input data))
          (is (empty? (changes ds :unit-changes)))
          (is (empty? (changes ds :exercise-changes))))  )))
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
                                    :where [:= :unit-id unit-id]})))  ))))
  (testing "that deleting a unit cascades changes for the unit and each exercise"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "delete-unit.json")
              mut (sut/use-mutation ds (changes/use-mutation ds))
              seeded (mapv (fn [seed] (sut/create (sut/use-mutation ds) seed))
                           (:seed data))
              exercise-uuids (mapv (comp :uuid first :exercises) seeded)]
          (sut/delete mut (:input data))
          (let [units (changes ds :unit-changes)
                exercises' (changes ds :exercise-changes)]
            (is (= 1 (count units)))
            (is (= "delete" (:change-type (first units))))
            ;; a delete change only carries identity, never the full record
            (is (= #{:uuid :course-uuid}
                   (set (keys (:change-data (first units))))))
            (is (= exercise-uuids
                   (mapv #(get-in % [:change-data :uuid]) exercises')))
            (is (every? #(= "delete" (:change-type %)) exercises'))
            (is (every? #(= (get-in (first units) [:change-data :uuid])
                            (get-in % [:change-data :unit-uuid]))
                        exercises'))  )))))
  (testing "that setting a unit field records a change for the unit"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-name.json")
              mut (sut/use-mutation ds (changes/use-mutation ds))
              seed (:unit (first (:seed data)))]
          (run! (partial insert-unit! ds) (:seed data))
          (sut/set-name mut {:id (:id seed) :name "renamed"})
          (let [[change] (changes ds :unit-changes)]
            (is (= "set-name" (:change-type change)))
            ;; :id is stripped from the change payload
            (is (= {:name "renamed"
                    :uuid (:uuid seed)
                    :course-uuid "test-course-uuid"}
                   (:change-data change))))))  ))

  (testing "that a set-course-id change is attributed to the course the unit moves to"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-name.json")
              mut (sut/use-mutation ds (changes/use-mutation ds))
              seed (:unit (first (:seed data)))]
          (run! (partial insert-unit! ds) (:seed data))
          (sut/set-course-id mut {:id (:id seed) :course-id 2})
          (let [[change] (changes ds :unit-changes)]
            ;; :course-id is stripped from the change payload, so the change has
            ;; to name the course the unit moved *to* for replay to work
            (is (= "set-course-id" (:change-type change)))
            (is (= "test-course-2-uuid" (get-in change [:change-data :course-uuid])))
            (is (not (contains? (:change-data change) :course-id)))))))  )

  (testing "that setting the name updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-name.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-name (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the description updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-description.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-description (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the level updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-level.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-level (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the type updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-type.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-type (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the course id updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-course-id.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-course-id (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
  (testing "that setting the position updates the unit"
    (with-test-db
      (fn [ds]
        (let [data (read-fixture "set-position.json")]
          (run! (partial insert-unit! ds) (:seed data))
          (let [actual (sut/set-position (sut/use-mutation ds) (:input data))]
            (is (= (:output data) actual))))  )))
)

(defn run-tests []
  (clojure.test/run-tests 'abantu.units-interface-test))
