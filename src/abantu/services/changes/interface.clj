(ns abantu.services.changes.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]))

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

(malt/defprotocol VersionControlQuery
  (lookup [input])
  (find [input])
  (all []))

(malt/defprotocol VersionControlMutation)

(defn use-query [])

(defn use-mutation [])
