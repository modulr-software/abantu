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
   [:review-pending :int [:default 0]]
   [:published-course-id :int]))

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

(def practice-sessions
  (tables/create-table-sql
   :practice-sessions
   (tables/table-id)
   [:user-id :int :not nil]
   [:unit-id :int :not nil]
   [:timestamp :int :not nil]))

(def exercises-completed
  (tables/create-table-sql
   :exercises-completed
   (tables/table-id)
   [:user-id :int :not nil]
   [:exercise-id :int :not nil]
   [:unit-id :int :not nil]
   [:correct :int [:default 0]]
   [:timestamp :int :not nil]
   [:practice-session-id :int]
   (tables/foreign-key :user-id :users :id)
   (tables/foreign-key :exercise-id :exercises :id)
   (tables/foreign-key :unit-id :units :id)
   (tables/foreign-key :practice-session-id :practice-sessions :id)))

(def comments
  (tables/create-table-sql
   :comments
   (tables/table-id)
   [:exercise-id :int :not nil]
   [:unit-id :int :not nil]
   [:course-id :int :not nil]
   [:text :text :not nil]
   [:user-id :int :not nil]
   [:timestamp :text :not nil]
   [:resolved :int [:default 0]]
   [:resolved-by :int]
   [:resolved-at :text]
   (tables/foreign-key :exercise-id :exercises :id)
   (tables/foreign-key :unit-id :units :id)
   (tables/foreign-key :course-id :courses :id)))

(def versions
  (tables/create-table-sql
   :versions
   (tables/table-id)
   [:label :text]
   [:course-id :int :not nil]
   [:version :int [:default 0]]
   [:applied :int [:default 0]]
   [:timestamp :text :not nil]))

(def course-changes
  (tables/create-table-sql
   :course-changes
   (tables/table-id)
   [:course-id :int :not nil]
   [:change-type :text :not nil]
   [:change-data :text :not nil]
   [:timestamp :text :not nil]
   [:version-id :int :not nil]
   (tables/foreign-key :course-id :courses :id)
   (tables/foreign-key :version-id :versions :id)))

(def unit-changes
  (tables/create-table-sql
   :unit-changes
   (tables/table-id)
   [:course-id :int :not nil]
   [:unit-id :int :not nil]
   [:change-type :text :not nil]
   [:change-data :text :not nil]
   [:timestamp :text :not nil]
   [:version-id :int :not nil]
   (tables/foreign-key :course-id :courses :id)
   (tables/foreign-key :unit-id :units :id)
   (tables/foreign-key :version-id :versions :id)))

(def exercise-changes
  (tables/create-table-sql
   :exercise-changes
   (tables/table-id)
   [:exercise-id :int :not nil]
   [:course-id :int :not nil]
   [:unit-id :int :not nil]
   [:change-type :text :not nil]
   [:change-data :text :not nil]
   [:timestamp :text :not nil]
   [:version-id :int :not nil]
   (tables/foreign-key :exercise-id :exercises :id)
   (tables/foreign-key :course-id :courses :id)
   (tables/foreign-key :unit-id :units :id)
   (tables/foreign-key :version-id :versions :id)))

(comment
  (sql/format events)
  (sql/format sessions)
  (sql/format session-answers)
  (sql/format courses)
  (sql/format units)
  (sql/format exercises)
  (sql/format answers)
  (sql/format practice-sessions)
  (sql/format exercises-completed)
  (sql/format comments)
  (sql/format versions)
  (sql/format course-changes)
  (sql/format unit-changes)
  (sql/format exercise-changes)
  ())
