(ns abantu.round-trip-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
            [abantu.db.honey :as hon]
            [abantu.services.courses.interface :as course]
            [abantu.services.units.interface :as unit]
            [abantu.services.exercises.interface :as exercise]
            [abantu.services.changes.interface :as changes]
            [abantu.services.versions.interface :as version]))

(defn read-fixture [file]
  (-> (io/resource (str "abantu/resources/round-trip/" file))
      (slurp)
      (json/read-value json/keyword-keys-object-mapper)))

(defn- norm
  "Ids are per-db sequences, so they can never be expected to match across the
   two databases - identity is carried by uuid. The student units table has no
   creator-id column, while master's does, so that key is absent on one side 
   and nil on the other."
  [courses]
  (mapv (fn [course']
          (-> course'
              (dissoc :id)
              (update :units (fn [units]
                               (mapv (fn [unit']
                                       (-> unit'
                                           (dissoc :id :creator-id)
                                           (update :exercises #(mapv (fn [e] (dissoc e :id)) %))))
                                     units)))))
        courses))

(defn- course-tree [ds]
  (norm (course/all (course/use-query ds))))

(defn- row-counts
  "counts of [courses units exercises] - the course tree reaches units and
   exercises through their parents, so these are what catch orphans the tree
   would hide"
  [ds]
  (mapv #(count (hon/find ds {:tname % :ret :*})) [:courses :units :exercises]))

(deftest test-round-trip
  (testing "that creating a course with units and exercises on the student db,
            then migrating up, reconstructs the same tree on master"
    (tu/with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "create-course.json")
              ;; the version must exist before the mutations
              v (version/create! (version/use-mutation student-ds)
                                 {:label "draft 1" :course-id 1})
              changes-mut (changes/use-mutation student-ds)
              courses-mut (course/use-mutation student-ds changes-mut)]
          ;; one call: the course -> unit -> exercise cascades run without a
          ;; changes api, and the single course change carries the whole tree
          (course/create courses-mut (:input data))
          (changes/migrate-up! (changes/use-mutation student-ds) v)
          ;; asserted against the fixture, so a field lost from the change 
          ;; feed cannot pass by being wrong on both sides
          (is (= (:expected data) (course-tree student-ds)))
          (is (= (:expected data) (course-tree master-ds)))))))

  (testing "that updating a preloaded course's units and exercises on the
            student db, then migrating up, reconstructs the same tree on master"
    (tu/with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "update-course.json")
              v (version/create! (version/use-mutation student-ds)
                                 {:label "draft 1" :course-id 1})
              cm (changes/use-mutation student-ds)
              um (unit/use-mutation student-ds cm)
              em (exercise/use-mutation student-ds cm)]

          ; preload test course data to be updated
          (course/create (course/use-mutation student-ds cm) (:input data))

          (unit/set-description um {:uuid "u1" :description "UPDATED desc"})
          (unit/set-level um {:uuid "u1" :level 3})
          (exercise/set-instruction em {:uuid "e1" :instruction "UPDATED instr"})
          (exercise/set-options em {:uuid "e1" :options ["a" "b" "c"]})
          (exercise/set-answers em {:uuid "e2" :answers [["x"] ["y"]]})
          (changes/migrate-up! (changes/use-mutation student-ds) v)
          (is (= (:expected data) (course-tree student-ds)))
          (is (= (:expected data) (course-tree master-ds))))))

    (testing "that deleting a unit on the student db, then migrating up, deletes
            that unit and its exercises on master too"
      (tu/with-test-dbs
        (fn [master-ds student-ds]
          (let [data (read-fixture "delete-unit.json")
                v (version/create! (version/use-mutation student-ds)
                                   {:label "draft 1" :course-id 1})
                cm (changes/use-mutation student-ds)]

            (course/create (course/use-mutation student-ds cm) (:input data))
            (unit/delete (unit/use-mutation student-ds cm) {:uuid "u1"})
            (changes/migrate-up! (changes/use-mutation student-ds) v)

            (is (= (:expected data) (course-tree student-ds)))
            (is (= (:expected data) (course-tree master-ds)))

            (is (= (set (:expected-exercise-uuids data))
                   (set (map :uuid (hon/find student-ds {:tname :exercises :ret :*})))))
            (is (= (set (:expected-exercise-uuids data))
                   (set (map :uuid (hon/find master-ds {:tname :exercises :ret :*}))))))))))

  (testing "that deleting a course that is already on master, then migrating up,
            deletes the course and its units and exercises on both dbs"
    (tu/with-test-dbs
      (fn [master-ds student-ds]
        (let [data (read-fixture "delete-course.json")
              vm (version/use-mutation student-ds)
              cm (changes/use-mutation student-ds)
              v1 (version/create! vm {:label "draft 1" :course-id 1})]

          (course/create (course/use-mutation student-ds cm) (:input data))
          (changes/migrate-up! cm v1)

          (is (= (:pre-delete-counts data) (row-counts student-ds)))
          (is (= (:pre-delete-counts data) (row-counts master-ds)))
          ;; this is needed for the next update to be separated from the first
          ;; as versions are differentiated by timestamp
          (Thread/sleep 1100)
          (let [v2 (version/create! vm {:label "draft 2" :course-id 1})]
            (course/delete (course/use-mutation student-ds cm) {:uuid "c-uuid"})
            (changes/migrate-up! cm v2)
            (is (= (:expected-counts data) (row-counts student-ds)))
            (is (= (:expected-counts data) (row-counts master-ds)))))))))

(defn run-tests []
  (clojure.test/run-tests 'abantu.round-trip-test))
