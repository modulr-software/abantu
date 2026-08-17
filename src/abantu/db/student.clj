(ns abantu.db.student
  (:require [abantu.db.tables :as tables]
            [honey.sql :as sql]))

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

(def courses
  (tables/create-table-sql
   :courses
   (tables/table-id)
   [:name :text]
   [:language :text]
   [:description :text]
   [:publishable :int [:default 0]]
   [:visible :int [:default 0]]
   [:review-pending :int [:default 0]]))

(def units
  (tables/create-table-sql
   :units
   (tables/table-id)
   [:name :text :not nil]
   [:description :text]
   [:level :int]
   [:type :text [:check [:in :type ["lesson" "practice"]]]]
   [:course-id :int]
   [:position :int]
   (tables/foreign-key :course-id :courses :id)))

(def exercises
  (tables/create-table-sql
   :exercises
   (tables/table-id)
   [:unit-id :integer :not nil]
   [:course-id :integer :not nil]
   [:instruction :text :not nil]
   [:question-content :text :not nil]
   [:audio :text]
   [:answer-type :text [:check [:in :answer-type ["freetext" "bubbles"]]]]
   [:options :text]
   [:level :int [:default 1]]
   [:correct-message :text]
   [:incorrect-message :text]
   [:position :int]
   (tables/foreign-key :unit-id :units :id)
   (tables/foreign-key :course-id :course :id)))

(def answers
  (tables/create-table-sql
   :answers
   (tables/table-id)
   [:exercise-id :int :not nil]
   [:text :text]
   [:audio :text]
   (tables/foreign-key :exercise-id :exercises :id)))

(comment
  (sql/format events)
  (sql/format sessions)
  (sql/format session-answers)
  (sql/format courses)
  (sql/format units)
  (sql/format exercises)
  (sql/format answers)
  ())
