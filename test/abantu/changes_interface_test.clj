(ns abantu.changes-interface-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [jsonista.core :as json]
            [abantu.test-util :as tu]
            [abantu.db.honey :as hon]
            [abantu.util :as util]
            [abantu.services.changes.interface :as sut]))

(defn with-test-student-db
  "Creates a fresh test master DB and a migrated test student DB, runs `f`
   with the open student connection, then cleans up the temp dir."
  ([f] (tu/with-test-student-db f))
  ([master-id student-id f] (tu/with-test-student-db master-id student-id f)))

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

(defn- keywordize-change-type [x]
  (cond
    (map? x) (reduce-kv (fn [acc k v]
                          (assoc acc k (if (= k :change-type)
                                         (keyword v)
                                         (keywordize-change-type v))))
                        x x)
    (vector? x) (mapv keywordize-change-type x)
    :else x))

(defn with-frozen-time [f]
  (with-redefs [util/get-utc-timestamp-string (constantly "2025-01-01T00:00:00Z")]
    (f)))

(defn- read-json [file]
  (-> (io/resource (str "abantu/resources/changes/" file))
      (slurp)
      (json/read-value json/keyword-keys-object-mapper)))

(defn read-fixture [file]
  (update (read-json file) :output keywordize-change-type))

(defn read-fixture-raw [file]
  (read-json file))

(deftest test-query
  (testing "that looking up one version by its id returns the version"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-by-id.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that looking up one version by its timestamp returns the version"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-by-timestamp.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that looking up a version that doesn't exist returns nil"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-missing.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that looking up one version by its id with :with-changes? returns the version with its changes attached"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-with-changes.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/lookup (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding versions by course id returns the versions"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-by-course-id.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding versions by label returns the matching versions"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-by-label.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/find (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding all versions returns a vec of versions"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "all.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/all (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding course changes within a timestamp range returns the grouped changes"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-course-changes-between.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/find-course-changes (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding unit changes from a timestamp returns the grouped changes"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-unit-changes-from.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/find-unit-changes (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that finding exercise changes from a timestamp returns the grouped changes"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "find-exercise-changes-from.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/find-exercise-changes (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that looking up one course change by its uuid returns the grouped course change"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-course-change.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/lookup-course-change (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that looking up one unit change by its uuid returns the grouped unit change"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-unit-change.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/lookup-unit-change (sut/use-query ds) (:query data))]
            (is (= (:output data) actual)))))))

  (testing "that looking up one exercise change by its uuid returns the grouped exercise change"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "lookup-exercise-change.json")]
          (run! (partial insert-seed! ds) (:seed data))
          (let [actual (sut/lookup-exercise-change (sut/use-query ds) (:query data))]
            (is (= (:output data) actual))))))))

(deftest test-mutation
  (testing "that creating a version returns the created version"
    (with-test-student-db
      (fn [ds]
        (with-frozen-time
          (fn []
            (let [data (read-fixture-raw "add-version.json")
                  actual (sut/add-version! (sut/use-mutation ds) (:input data))]
              (is (= (:output data) actual))))))))

  (testing "that creating a version without a label returns the created version"
    (with-test-student-db
      (fn [ds]
        (with-frozen-time
          (fn []
            (let [data (read-fixture-raw "add-version-no-label.json")
                  actual (sut/add-version! (sut/use-mutation ds) (:input data))]
              (is (= (:output data) actual))))))))

  (testing "that setting the version label updates the version"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-version-label.json")
              _ (run! (partial insert-seed! ds) (:seed data))
              actual (sut/set-version-label! (sut/use-mutation ds) (:input data))]
          (is (= (:output data) actual))))))

  (testing "that setting the version label with :with-changes? returns the version with its changes attached"
    (with-test-student-db
      (fn [ds]
        (let [data (read-fixture "set-version-label-with-changes.json")
              _ (run! (partial insert-seed! ds) (:seed data))
              actual (sut/set-version-label! (sut/use-mutation ds) (:input data))]
          (is (= (:output data) actual))))))

  (testing "that adding a course update records a create change"
    (with-test-student-db
      (fn [ds]
        (with-frozen-time
          (fn []
            (let [data (read-fixture-raw "add-course-update.json")
                  actual (sut/add-course-update! (sut/use-mutation ds) (:input data))]
              (is (= (:output data)
                     (update actual :change-data
                             #(json/read-value % json/keyword-keys-object-mapper))))))))))

  (testing "that adding a unit update records a create change"
    (with-test-student-db
      (fn [ds]
        (with-frozen-time
          (fn []
            (let [data (read-fixture-raw "add-unit-update.json")
                  actual (sut/add-unit-update! (sut/use-mutation ds) (:input data))]
              (is (= (:output data)
                     (update actual :change-data
                             #(json/read-value % json/keyword-keys-object-mapper))))))))))

  (testing "that adding an exercise update records a create change"
    (with-test-student-db
      (fn [ds]
        (with-frozen-time
          (fn []
            (let [data (read-fixture-raw "add-exercise-update.json")
                  actual (sut/add-exercise-update! (sut/use-mutation ds) (:input data))]
              (is (= (:output data)
                     (update actual :change-data
                             #(json/read-value % json/keyword-keys-object-mapper)))))))))))

(defn run-tests []
  (clojure.test/run-tests 'abantu.changes-interface-test))
