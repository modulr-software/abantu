(ns abantu.exercises-interface-test
  (:require [clojure.test :refer :all]
            [abantu.db-test-utils :as db-test-utils]
            [abantu.db.honey :as hon]
            [abantu.services.exercises.interface :as exercises-intf-sut]))

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

(deftest create-test
  (testing "manual control insert and interface create leave identical db contents"
    (let [input {:unit-id 1
                 :course-id 1
                 :instruction "Translate the following"
                 :question-content "Who are you?"
                 :answer-type "bubbles"
                 :options ["wat" "wie" "hoe"]
                 :correct-message "correct!"
                 :incorrect-message "o nei"
                 :answers [["wie" "is"]]}
          created (exercises-intf-sut/create @em input)
          ;; control insert mirrors exactly what -create writes to the db
          _inserted-exercise (hon/insert! @control-ds
                                          {:tname :exercises
                                           :values {:uuid (:uuid created)
                                                    :unit-id 1
                                                    :course-id 1
                                                    :instruction "Translate the following"
                                                    :question-content "Who are you?"
                                                    :answer-type "bubbles"
                                                    :options "wat;;wie;;hoe"
                                                    :correct-message "correct!"
                                                    :incorrect-message "o nei"}})
          _inserted-answer (hon/insert! @control-ds
                                        {:tname :answers
                                         :values {:exercise-id (:id created)
                                                  :text "wie;;is"}})]
      (is (= (hon/find @control-ds {:tname :exercises :ret :*})
             (hon/find @sut-ds {:tname :exercises :ret :*})))
      (is (= (hon/find @control-ds {:tname :answers :ret :*})
             (hon/find @sut-ds {:tname :answers :ret :*}))))))

(deftest delete-test
  (testing "delete removes the exercise and all its dependent rows"
    (let [input {:unit-id 1
                 :course-id 1
                 :instruction "Translate the following"
                 :question-content "Who are you?"
                 :answer-type "bubbles"
                 :options ["wat" "wie" "hoe"]
                 :correct-message "correct!"
                 :incorrect-message "o nei"
                 :answers [["wie"] ["hoe"]]}
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