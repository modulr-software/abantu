(ns abantu.services.changes.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]))

(def ?Version
  [:map
   [:id :int]
   [:timestamp :text]
   [:label :text]
   [:user-id :int]
   [:course-id :int]])

(def ?CourseChanges
  [:map
   [:course-id :int]
   [:change-type :text]
   [:change-data :text]
   [:timestamp :text]])

(def ?UnitChanges
  [:map
   [:course-id :int]
   [:unit-id :int]
   [:change-type :text]
   [:change-data :text]
   [:timestamp :text]])

(def ?ExerciseChanges
  [:map
   [:exercise-id :int]
   [:course-id :int]
   [:unit-id :int]
   [:change-type :text]
   [:change-data :text]
   [:timestamp :text]])

(def ?Lookup
  (mu/ []))

(malt/defprotocol VersionControlQuery
  (lookup [input ])
  (find [input])
  (all [])
  (exercises []))

(malt/defprotocol VersionControlMutation)

(defn use-query [])

(defn use-mutation [])
