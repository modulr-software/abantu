(ns abantu.services.changes.interface
  (:require [abantu.services.changes.core :as changes]
            [abantu.util :as util]
            [io.julienvincent.malt :as malt]
            [malli.util :as mu]))

(def ?CourseChange
  [:map
   [:course-uuid :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]
   [:version-id :int]])

(def ?UnitChange
  [:map
   [:course-uuid :int]
   [:unit-uuid :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]
   [:version-id :int]])

(def ?ExerciseChange
  [:map
   [:exercise-uuid :int]
   [:course-uuid :int]
   [:unit-uuid :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]
   [:version-id :int]])

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

(def ?ChangeLookup
  [:or
   (mu/merge [:map [:version-id :int]] ?Opts)
   (mu/merge [:map [:timestamp :string]] ?Opts)])

(def ?AddVersion
  (mu/select-keys ?Version [:label :course-id]))

(def ?SetVersionLabel
  (mu/select-keys ?Version [:id :label]))

(def ?AddCourseUpdate
  (-> (mu/select-keys ?CourseChange [:version-id :change-type])
      (mu/assoc :update [:map [:id :int]])))

(def ?AddUnitUpdate
  (-> (mu/select-keys ?UnitChange [:version-id :change-type])
      (mu/assoc :update [:map [:id :int]])))

(def ?AddExerciseUpdate
  (-> (mu/select-keys ?ExerciseChange [:version-id :change-type])
      (mu/assoc :update [:map [:id :int]])))

(def ?MigrateUp
  (mu/select-keys ?Version [:id :applied :course-id]))

(malt/defprotocol VersionControlQuery
  (lookup [input ?Lookup]
    (util/maybe ?Version))
  (find [input ?Find]
    [:vector ?Version])
  (all [input ?Opts]
    [:vector ?Version])
  (exercises [input ?ChangeLookup]
    [:vector ?ExerciseChange])
  (units [input ?ChangeLookup]
    [:vector ?UnitChange])
  (courses [input ?ChangeLookup]
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
    (exercises [_ input]
      (changes/-exercises ds input))
    (units [_ input]
      (changes/-units ds input))
    (courses [_ input]
      (changes/-courses ds input))))

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
      (changes/migrate-up! ds input))))

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

  (exercises vcq {:version-id 1})
  (exercises vcq {:timestamp "2025-01-01T00:00:00Z"})
  (units vcq {:version-id 1})
  (units vcq {:timestamp "2025-01-01T00:00:00Z"})
  (courses vcq {:version-id 1})
  (courses vcq {:timestamp "2025-01-01T00:00:00Z"})

  ;; mutations

  (add-version! vcm {:label "draft 1"
                     :course-id 1})
  (add-version! vcm {:course-id 1})

  (set-version-label! vcm {:id 1
                           :label "review draft"})

  (add-course-update! vcm {:version-id 1
                           :change-type "set-name"
                           :update {:id 1
                                    :name "afrikaans basics"}})

  (add-unit-update! vcm {:version-id 1
                         :change-type "create"
                         :update {:id 1
                                  :course-id 1
                                  :name "pronouns"
                                  :type "lesson"}})

  (add-exercise-update! vcm {:version-id 1
                             :change-type "delete"
                             :update {:id 1
                                      :unit-id 1
                                      :course-id 1}})

  #_(migrate-up! vcm {:course-id 1
                      :version-id 1})

  :end)
