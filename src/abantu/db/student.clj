(ns abantu.db.student
  (:require [abantu.db.tables :as tables]))

(def events
  (tables/create-table-sql
   :events
   (tables/table-id)
   [:unit-id :int]
   [:course-id :int]
   [:exercise-id :int]
   [:event-type :text :not nil]
   [:event-data :text :not nil]))