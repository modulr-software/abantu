(ns abantu.services.changes.interface
  (:require [abantu.util :as util]
            [io.julienvincent.malt :as malt]
            [malli.util :as mu]))

(def ?Version
  [:map
   [:id :int]
   [:timestamp :string]
   [:label :string]
   [:user-id :int]
   [:course-id :int]])

(def ?CourseChanges
  [:map
   [:course-id :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]])

(def ?UnitChanges
  [:map
   [:course-id :int]
   [:unit-id :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]])

(def ?ExerciseChanges
  [:map
   [:exercise-id :int]
   [:course-id :int]
   [:unit-id :int]
   [:change-type :string]
   [:change-data :string]
   [:timestamp :string]])

(def ?Lookup
  [:or
   [:map [:id :int]]
   [:map [:timestamp :string]]])

(def ?Find
  ?Lookup)

(malt/defprotocol VersionControlQuery
  (lookup [input [:map [:id :int]]]
    (util/maybe ?Version))
  (find [input ?Find]
    [:vector ?Version])
  (all []
    [:vector ?Version])
  (exercises []
    [:vector ?ExerciseChanges])
  (units []
    [:vector ?UnitChanges])
  (courses []
    [:vector ?CourseChanges]))

(malt/defprotocol VersionControlMutation)

(defn use-query [])

(defn use-mutation [])
