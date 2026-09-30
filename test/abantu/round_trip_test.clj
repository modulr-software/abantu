(ns abantu.round-trip-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
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
          (is (= (:expected data) (course-tree master-ds))))))))

(defn run-tests []
  (clojure.test/run-tests 'abantu.round-trip-test))
