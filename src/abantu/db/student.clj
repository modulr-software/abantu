(ns abantu.db.student
  (:require [abantu.db.tables :as tables]))

(def events
  (tables/create-table-sql
   :events
   (tables/table-id)
   [:unit-id :int]
   [:course-id :int]
   [:exercise-id :int]
   [:timestamp :text :not nil]
   [:event-type :text :not nil]
   [:event-data :text [:default ""]]))

(def sessions
  (tables/create-table-sql
   :sessions
   (tables/table-id)
   [:unit-id :int :not nil]
   [:course-id :int :not nil]
   [:completed :int [:default 0]]
   [:started-at :text :not nil]
   [:ended-at :text]))

(def session-answers
  (tables/create-table-sql
   :session-answers
   (tables/table-id)
   [:unit-id :int :not nil]
   [:course-id :int :not nil]
   [:exercise-id :int :not nil]
   [:answer :text]
   [:correct :int [:default 0]]
   [:started-at :text]
   [:ended-at :text]
   [:session-id :int :not nil]
   (tables/foreign-key :session-id :sessions :id)))
