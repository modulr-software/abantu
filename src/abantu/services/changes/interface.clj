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
              [:uuid :int]
              [:change-type :string]
              [:change-data :any]
              [:timestamp :string]]]]])

(def ?UnitChange
  [:map
   [:uuid :string]
   [:change [:vector
             [:map
              [:uuid :int]
              [:course-uuid :int]
              [:change-type :string]
              [:change-data :any]
              [:timestamp :string]]]]])

(def ?ExerciseChange
  [:map
   [:uuid :string]
   [:change [:vector
             [:map
              [:uuid :int]
              [:course-uuid :int]
              [:unit-uuid :int]
              [:change-type :string]
              [:change-data :any]
              [:timestamp :string]]]]])

(def ?Version
  [:map
   [:id :int]
   [:timestamp :string]
   [:label {:optional true} (util/maybe :string)]
   [:applied :boolean]
   [:course-id :int]
   [:course-changes [:vector ?CourseChange]]
   [:unit-changes [:vector ?UnitChange]]
   [:exercise-changes [:vector ?ExerciseChange]]])

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
   [:to (util/maybe :string)]])

(def ?ChangeLookup
  [:map
   [:uuid (util/maybe :string)]
   [:change-type (util/maybe :string)]
   [:timestamp (util/maybe :string)]])

(def ?AddVersion
  (mu/select-keys ?Version [:label :course-id]))

(def ?SetVersionLabel
  (mu/select-keys ?Version [:id :label]))

(def ?AddCourseUpdate
  (-> (mu/select-keys ?CourseChange [:version-id :change-type])
      (mu/assoc :update [:map [:uuid :int]])))

(def ?AddUnitUpdate
  (-> (mu/select-keys ?UnitChange [:version-id :change-type])
      (mu/assoc :update [:map
                         [:uuid :string]
                         [:course-uuid :string]])))

(def ?AddExerciseUpdate
  (-> (mu/select-keys ?ExerciseChange [:version-id :change-type])
      (mu/assoc :update [:map
                         [:uuid :string]
                         [:unit-uuid :string]
                         [:course-uuid :string]])))

(def ?MigrateUp
  (mu/select-keys ?Version [:id :applied :course-id]))

(malt/defprotocol VersionControlQuery
  (lookup [input ?Lookup]
    (util/maybe ?Version))
  (find [input ?Find]
    [:vector ?Version])
  (all [input ?Opts]
    [:vector ?Version])
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

(malt/defprotocol VersionControlMutation
  (add-version! [input ?AddVersion]
    (util/maybe ?Version))
  (set-version-label! [input ?SetVersionLabel]
    (util/maybe ?Version))
  (add-course-update! [input ?AddCourseUpdate]
    (util/maybe ?CourseChange))
  (add-unit-update! [input ?AddUnitUpdate]
    (util/maybe ?UnitChange))
  (add-exercise-update! [input ?AddExerciseUpdate]
    (util/maybe ?ExerciseChange))
  (migrate-up! [input ?MigrateUp]
    :nil))

(defn use-query [ds]
  (reify VersionControlQuery
    (lookup [_ input]
      (changes/-lookup ds input))
    (find [_ input]
      (changes/-find ds input))
    (all [_ input]
      (changes/-all ds input))
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
  (reify VersionControlMutation
    (add-version! [_ input]
      (changes/-add-version! ds input))
    (set-version-label! [_ input]
      (changes/-set-version-label! ds input))
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

  (lookup vcq {:id 1})
  (lookup vcq {:id 1 :with-changes? true})
  (lookup vcq {:timestamp "2025-01-01T00:00:00Z"
               :with-changes? true})

  (find vcq {:course-id 1})
  (find vcq {:course-id 1 :with-changes? true})
  (find vcq {:label "draft 1"
             :with-changes? true})

  (all vcq {:with-changes? false})
  (all vcq {:with-changes? true})

  (find-exercise-changes vcq {:version-id 1})
  (find-exercise-changes vcq {:timestamp "2025-01-01T00:00:00Z"})
  (find-unit-changes vcq {:version-id 1})
  (find-unit-changes vcq {:timestamp "2025-01-01T00:00:00Z"})
  (find-course-changes vcq {:version-id 1})
  (find-course-changes vcq {:timestamp "2025-01-01T00:00:00Z"})

  ;; mutations

  (add-version! vcm {:label "draft 1"
                     :course-id 1})
  (add-version! vcm {:course-id 1})

  (set-version-label! vcm {:id 1
                           :label "review draft"})

  (add-course-update!
   vcm
   {:version-id 1
    :change-type "create"
    :update {:id 1
             :uuid "5fe04376aa57d644"
             :name "zulu basics"
             :language "zulu"
             :description "learn zulu"}})

  (add-course-update! vcm {:version-id 1
                           :change-type "create"
                           :update {:id 2
                                    :uuid "8632b03c31aebe91"
                                    :name "afrikaans"
                                    :language "afrikaans"
                                    :description "learn afrikaans"
                                    :units []}})

  (add-course-update! vcm {:version-id 2
                           :change-type "set-description"
                           :update {:id 1
                                    :uuid "5fe04376aa57d644"
                                    :description "lets maybe use an actual description here"}})

  (add-unit-update! vcm {:version-id 1
                         :change-type "create"
                         :update {:id 1
                                  :uuid "88bbb7209549523f"
                                  :course-id 1
                                  :course-uuid "5fe04376aa57d644"
                                  :name "pronouns 1"
                                  :description "useful stuff"
                                  :type "lesson"
                                  :exercises []}})

  (add-unit-update! vcm {:version-id 2
                         :change-type "set-description"
                         :update {:id 1
                                  :uuid "88bbb7209549523f"
                                  :course-uuid "5fe04376aa57d644"
                                  :description "unit about pronouns"}})

  (add-exercise-update! vcm {:version-id 1
                             :change-type "create"
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

  (add-exercise-update! vcm {:version-id 2
                             :change-type "set-instruction"
                             :update {:id 1
                                      :uuid "529849abbecc2114"
                                      :unit-id 1
                                      :unit-uuid "88bbb7209549523f"
                                      :course-id 1
                                      :course-uuid "5fe04376aa57d644"
                                      :instruction "Translate the following:"}})

  #_(migrate-up! vcm {:course-id 1
                      :version-id 1})

  :end)
