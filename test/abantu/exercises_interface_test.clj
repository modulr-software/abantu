(ns abantu.exercises-interface-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [abantu.db-test-utils :as db-test-utils]
            [abantu.db.honey :as hon]
            [abantu.services.exercises.interface :as exercises-intf-sut]))

(def exercises-data
  (-> (io/resource "abantu/exercises.json")
      slurp
      (json/read-str {:key-fn keyword})))

(def control-ds (atom nil))
(def sut-ds (atom nil))
(def eq (atom nil))
(def em (atom nil))

(use-fixtures :each
  (fn [f]
    (reset! control-ds (db-test-utils/create-test-db! 1))
    (reset! sut-ds (db-test-utils/create-test-db! 2))
    (reset! eq (exercises-intf-sut/use-query @sut-ds))
    (reset! em (exercises-intf-sut/use-mutation @sut-ds))
    (try (f)
         (finally
           (db-test-utils/cleanup-test-db! 1)
           (db-test-utils/cleanup-test-db! 2)))))

(defn mirror-create!
  "Insert into control-ds exactly the rows -create writes for `input`."
  [input created]
  (hon/insert! @control-ds
               {:tname :exercises
                :values {:uuid (:uuid created)
                         :unit-id (:unit-id input)
                         :course-id (:course-id input)
                         :instruction (:instruction input)
                         :question-content (:question-content input)
                         :answer-type (:answer-type input)
                         :options (str/join ";;" (:options input))
                         :correct-message (:correct-message input)
                         :incorrect-message (:incorrect-message input)}})
  (when (seq (:answers input))
    (hon/insert! @control-ds
                 {:tname :answers
                  :values (mapv #(assoc {:text (str/join ";;" %)}
                                        :exercise-id (:id created))
                                (:answers input))})))

(defn seed-exercise!
  "Create `input` via the interface in sut-ds and mirror its rows into control-ds."
  [input]
  (let [created (exercises-intf-sut/create @em input)]
    (mirror-create! input created)
    created))

(defn assert-control-matches!
  "Assert the exercises and answers tables are identical between the two dbs."
  []
  (is (= (hon/find @control-ds {:tname :exercises :ret :*})
         (hon/find @sut-ds {:tname :exercises :ret :*})))
  (is (= (hon/find @control-ds {:tname :answers :ret :*})
         (hon/find @sut-ds {:tname :answers :ret :*}))))

(deftest create-test
  (testing "manual control insert and interface create leave identical db contents"
    (let [input (exercises-data :bubbles-basic)
          _created (seed-exercise! input)]
      (assert-control-matches!))))

(deftest delete-test
  (testing "delete removes the exercise and all its dependent rows"
    (let [input (exercises-data :bubbles-multi)
          created (exercises-intf-sut/create @em input)
          id (:id created)
          _comment (hon/insert! @sut-ds {:tname :comments
                                         :values {:exercise-id id
                                                  :unit-id 1
                                                  :course-id 1
                                                  :text "comment"
                                                  :user-id 1
                                                  :timestamp "2026-01-01T00:00:00Z"}})
          _completed (hon/insert! @sut-ds {:tname :exercises-completed
                                           :values {:user-id 1
                                                    :exercise-id id
                                                    :unit-id 1
                                                    :timestamp 123456}})
          _deleted (exercises-intf-sut/delete @em {:id id})]
      (is (empty? (hon/find @sut-ds {:tname :exercises
                                     :where [:= :id id]
                                     :ret :*})))
      (is (empty? (hon/find @sut-ds {:tname :answers
                                     :where [:= :exercise-id id]
                                     :ret :*})))
      (is (empty? (hon/find @sut-ds {:tname :comments
                                     :where [:= :exercise-id id]
                                     :ret :*})))
      (is (empty? (hon/find @sut-ds {:tname :exercises-completed
                                     :where [:= :exercise-id id]
                                     :ret :*}))))))

(deftest lookup-test
  (testing "lookup returns the seeded exercise by id and by uuid"
    (let [input (exercises-data :bubbles-basic)
          created (seed-exercise! input)
          by-id (exercises-intf-sut/lookup @eq {:id (:id created)})
          by-uuid (exercises-intf-sut/lookup @eq {:uuid (:uuid created)})]
      (assert-control-matches!)
      (is (= (:id created) (:id by-id)))
      (is (= (:uuid created) (:uuid by-id)))
      (is (= (:id created) (:id by-uuid)))
      (is (= (:uuid created) (:uuid by-uuid))))))

(deftest find-test
  (testing "find returns exercises by course and by unit"
    (let [basic (seed-exercise! (exercises-data :bubbles-basic))
          unit-two (seed-exercise! (exercises-data :bubbles-unit-two))]
      (assert-control-matches!)
      (is (= 2 (count (exercises-intf-sut/find @eq {:course-id 1}))))
      (is (= #{(:id basic)}
             (set (map :id (exercises-intf-sut/find @eq {:unit-id 1})))))
      (is (= #{(:id unit-two)}
             (set (map :id (exercises-intf-sut/find @eq {:unit-id 2}))))))))

(deftest all-test
  (testing "all returns every seeded exercise"
    (let [basic (seed-exercise! (exercises-data :bubbles-basic))
          unit-two (seed-exercise! (exercises-data :bubbles-unit-two))]
      (assert-control-matches!)
      (is (= #{(:id basic) (:id unit-two)}
             (set (map :id (exercises-intf-sut/all @eq))))))))
