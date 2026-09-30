(ns abantu.services.changes.interface
  (:require [abantu.services.changes.core :as changes]
            [abantu.services.changes.migrate :as migrate]
            [abantu.util :as util]
            [io.julienvincent.malt :as malt]
            [malli.util :as mu]))

(def ?CourseChange
  [:map
   [:uuid :string]
   [:change [:vector
             [:map
              [:uuid :string]
              [:change-type :string]
              [:change-data :any]
              [:timestamp :string]]]]])

(def ?UnitChange
  [:map
   [:uuid :string]
   [:change [:vector
             [:map
              [:uuid :string]
              [:course-uuid :string]
              [:change-type :string]
              [:change-data :any]
              [:timestamp :string]]]]])

(def ?ExerciseChange
  [:map
   [:uuid :string]
   [:change [:vector
             [:map
              [:uuid :string]
              [:course-uuid :string]
              [:unit-uuid :string]
              [:change-type :string]
              [:change-data :any]
              [:timestamp :string]]]]])

(def ?Opts
  [:map [:with-changes? :boolean]])

(def ?Lookup
  [:or
   (mu/merge [:map [:id :int]] ?Opts)
   (mu/merge [:map [:timestamp :string]] ?Opts)])

(def ?Find
  [:or
   (mu/merge [:map [:course-id :int]] ?Opts)
   (mu/merge [:map [:label :string]] ?Opts)])

(def ?ChangeFind
  [:map
   [:from (util/maybe :string)]
   [:to (util/maybe :string)]
   [:change-type (util/maybe :string)]
   [:change-data (util/maybe :string)]])

(def ?ChangeLookup
  [:map
   [:uuid (util/maybe :string)]
   [:change-type (util/maybe :string)]
   [:timestamp (util/maybe :string)]])

(def ?AddCourseUpdate
  (-> (mu/select-keys ?CourseChange [:change-type])
      (mu/assoc :update [:map [:uuid :string]])))

(def ?AddUnitUpdate
  (-> (mu/select-keys ?UnitChange [:change-type])
      (mu/assoc :update [:map
                         [:uuid :string]
                         [:course-uuid :string]])))

(def ?AddExerciseUpdate
  (-> (mu/select-keys ?ExerciseChange [:change-type])
      (mu/assoc :update [:map
                         [:uuid :string]
                         [:unit-uuid :string]
                         [:course-uuid :string]])))

(def ?MigrateUp
  [:map
   [:id :int]
   [:course-id :int]
   [:applied :boolean]])

(malt/defprotocol ChangesQuery
  (lookup-exercise-change [input ?ChangeLookup]
    (util/maybe ?ExerciseChange))
  (lookup-unit-change [input ?ChangeLookup]
    (util/maybe ?UnitChange))
  (lookup-course-change [input ?ChangeLookup]
    (util/maybe ?CourseChange))
  (find-exercise-changes [input ?ChangeFind]
    [:vector ?ExerciseChange])
  (find-unit-changes [input ?ChangeFind]
    [:vector ?UnitChange])
  (find-course-changes [input ?ChangeFind]
    [:vector ?CourseChange]))

(malt/defprotocol ChangesMutation
  (add-course-update! [input ?AddCourseUpdate]
    (util/maybe ?CourseChange))
  (add-unit-update! [input ?AddUnitUpdate]
    (util/maybe ?UnitChange))
  (add-exercise-update! [input ?AddExerciseUpdate]
    (util/maybe ?ExerciseChange))
  (migrate-up! [input ?MigrateUp]
    :nil))

(defn use-query [ds]
  (reify ChangesQuery
    (lookup-exercise-change [_ input]
      (changes/-lookup-exercise-change ds input))
    (lookup-unit-change [_ input]
      (changes/-lookup-unit-change ds input))
    (lookup-course-change [_ input]
      (changes/-lookup-course-change ds input))
    (find-exercise-changes [_ input]
      (changes/-find-exercise-changes ds input))
    (find-unit-changes [_ input]
      (changes/-find-unit-changes ds input))
    (find-course-changes [_ input]
      (changes/-find-course-changes ds input))))

(defn use-mutation [ds]
  (reify ChangesMutation
    (add-course-update! [_ input]
      (changes/-add-course-update! ds input))
    (add-unit-update! [_ input]
      (changes/-add-unit-update! ds input))
    (add-exercise-update! [_ input]
      (changes/-add-exercise-update! ds input))
    (migrate-up! [_ input]
      (migrate/migrate-up! ds input))))

(comment

  (require '[abantu.db.util :as db.util])
  (def ds (db.util/conn :student 1))

  (def vcq (use-query ds))
  (def vcm (use-mutation ds))

  ;; queries
  (find-exercise-changes vcq {:from "2025-01-01T00:00:00Z"
                              :to "2025-01-31T00:00:00Z"})
  (find-unit-changes vcq {:from "2025-01-01T00:00:00Z"
                          :to "2025-01-31T00:00:00Z"})
  (find-course-changes vcq {:from "2025-01-01T00:00:00Z"
                            :to "2025-01-31T00:00:00Z"})

  ;; mutations
  (add-course-update!
   vcm
   {:change-type "create"
    :update {:id 1
             :uuid "5fe04376aa57d644"
             :name "zulu basics"
             :language "zulu"
             :description "learn zulu"
             :units [{:id 1
                      :uuid "88bbb7209549523f"
                      :course-id 1
                      :course-uuid "5fe04376aa57d644"
                      :name "pronouns 1"
                      :description "useful stuff"
                      :type "lesson"
                      :exercises []}]}})

  (add-course-update!
   vcm
   {:change-type "create"
    :update {:uuid "bf02418ff6342d18",
             :id 1,
             :description "learn afrikaans",
             :creator nil,
             :name "afrikaans basics",
             :language "afrikaans",
             :publishable false,
             :visible false,
             :review-pending false
             :units [{:uuid "472a75c9f292632f",
                      :id 1,
                      :description "intro to pronouns",
                      :name "pronouns 1",
                      :type "lesson",
                      :level nil,
                      :position nil,
                      :course-id 1
                      :exercises [{:uuid "f49160fb67a86f42",
                                   :id 1,
                                   :question-content "who are you",
                                   :correct-message "correct!",
                                   :comments [],
                                   :unit-id 1,
                                   :level 1,
                                   :answers [["wie" "is" "jy"]],
                                   :position nil,
                                   :options ["wat" "wie" "hoe" "is" "jy"],
                                   :course-id 1,
                                   :answer-type "bubbles",
                                   :instruction "translate the following",
                                   :incorrect-message "o nei"}]}]}})

  (add-course-update! vcm {:change-type "create"
                           :update {:id 2
                                    :uuid "8632b03c31aebe91"
                                    :name "afrikaans"
                                    :language "afrikaans"
                                    :description "learn afrikaans"
                                    :units []}})

  (add-course-update! vcm {:change-type "delete"
                           :update {:id 2
                                    :uuid "8632b03c31aebe91"}})

  (add-course-update! vcm {:change-type "set-description"
                           :update {:id 1
                                    :uuid "5fe04376aa57d644"
                                    :description "lets maybe use an actual description here"}})

  (add-unit-update! vcm {:change-type "create"
                         :update {:id 1
                                  :uuid "88bbb7209549523f"
                                  :course-id 1
                                  :course-uuid "5fe04376aa57d644"
                                  :name "pronouns 1"
                                  :description "useful stuff"
                                  :type "lesson"
                                  :exercises []}})

  (add-unit-update! vcm {:change-type "set-description"
                         :update {:id 1
                                  :uuid "88bbb7209549523f"
                                  :course-uuid "5fe04376aa57d644"
                                  :description "unit about pronouns"}})

  (add-exercise-update! vcm {:change-type "create"
                             :update {:id 1
                                      :uuid "529849abbecc2114"
                                      :unit-id 1
                                      :unit-uuid "88bbb7209549523f"
                                      :course-id 1
                                      :course-uuid "5fe04376aa57d644"
                                      :instruction "translate the following"
                                      :question-content "who are you"
                                      :answer-type "bubbles"
                                      :options ["wat" "wie" "hoe" "is" "jy"]
                                      :correct-message "correct!"
                                      :incorrect-message "o nei"
                                      :answers [["wie" "is" "jy"]]}})

  (add-exercise-update! vcm {:change-type "set-instruction"
                             :update {:id 1
                                      :uuid "529849abbecc2114"
                                      :unit-id 1
                                      :unit-uuid "88bbb7209549523f"
                                      :course-id 1
                                      :course-uuid "5fe04376aa57d644"
                                      :instruction "Translate the following:"}})

  #_(migrate-up! vcm {:course-id 1
                      :id 1})

  :end)
