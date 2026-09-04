(ns abantu.services.changes.interface
  (:require [abantu.util :as util]
            [io.julienvincent.malt :as malt]
            [malli.util :as mu]))

(def ?CourseChange
  [:map
   [:course-id :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]
   [:version-id :int]])

(def ?UnitChange
  [:map
   [:course-id :int]
   [:unit-id :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]
   [:version-id :int]])

(def ?ExerciseChange
  [:map
   [:exercise-id :int]
   [:course-id :int]
   [:unit-id :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]
   [:version-id :int]])

(def ?Version
  [:map
   [:id :int]
   [:timestamp :string]
   [:label {:optional true} (util/maybe :string)]
   [:user-id :int]
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
  ?Lookup)

(def ?AddVersion
  (mu/select-keys ?Version [:label :user-id :course-id]))

(def ?SetVersionLabel
  (mu/select-keys ?Version [:id :label]))

(def ?AddCourseUpdate
  (-> (mu/select-keys ?CourseChange [:version-id])
      (mu/assoc :update [:map [:id :int]])))

(def ?AddUnitUpdate
  (-> (mu/select-keys ?UnitChange [:version-id])
      (mu/assoc :update [:map [:id :int]])))

(def ?AddExerciseUpdate
  (-> (mu/select-keys ?ExerciseChange [:version-id])
      (mu/assoc :update [:map [:id :int]])))

(def ?MigrateUp
  [:map
   [:course-id :int]
   [:version-id :int]])

(malt/defprotocol VersionControlQuery
  (lookup [input ?Lookup]
    (util/maybe ?Version))
  (find [input ?Find]
    [:vector ?Version])
  (all [input ?Opts]
    [:vector ?Version])
  (exercises [input ?Lookup]
    [:vector ?ExerciseChange])
  (units [input ?Lookup]
    [:vector ?UnitChange])
  (courses [input ?Lookup]
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

(defn use-query [])

(defn use-mutation [])
