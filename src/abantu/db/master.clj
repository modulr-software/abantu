(ns abantu.db.master
  (:require [abantu.db.tables :as tables]))

(def users
  (tables/create-table-sql
   :users
   (tables/table-id)
   [:email :text]
   [:password :text]
   [:firstname :text]
   [:lastname :text]
   [:email-verified :integer [:default 0]]
   [:mobile :text]
   [:email-hash :string]
   [:profile-image :text]
   [:user-type-id :int]
   (tables/foreign-key :user-type-id :user-types :id)))

(def devices
  (tables/create-table-sql
   :devices
   (tables/table-id)
   [:uuid :text]
   [:user-id :int]
   (tables/foreign-key :user-id :users :id)))

(def user-types
  (tables/create-table-sql
   :user-types
   (tables/table-id)
   [:name :text [:check [:in :name ["student" "creator" "admin"]]]]))

(def user-assigned-types
  (tables/create-table-sql
   :user-assigned-types
   (tables/table-id)
   [:user-id :integer]
   [:user-type-id :integer]
   (tables/foreign-key :user-id :users :id)
   (tables/foreign-key :user-type-id :user-types :id)))

(def vocab
  (tables/create-table-sql
   :vocab
   (tables/table-id)
   [:xhosa :text :not nil]
   [:english :text :not nil]
   [:type :text :not nil]
   [:noun-class :text]))

(def courses
  (tables/create-table-sql
   :courses
   (tables/table-id)
   [:name :text]
   [:language :text]
   [:status :text [:check [:in :status  ["in-progress" "review" "published"]]]]
   [:creator-id :int]
   (tables/foreign-key :creator-id :users :id)))

(def user-courses
  (tables/create-table-sql
   :user-courses
   (tables/table-id)
   [:user-id :int :not nil]
   [:course-id :int :not nil]
   (tables/foreign-key :user-id :users :id)
   (tables/foreign-key :course-id :courses :id)))

(def units
  (tables/create-table-sql
   :units
   (tables/table-id)
   [:name :text :not nil]
   [:description :text]
   [:level :int]
   [:course-id :int]
   (tables/foreign-key :course-id :courses :id)))

(def exercises
  (tables/create-table-sql
   :exercises
   (tables/table-id)
   [:unit-id :integer :not nil]
   [:question-type :text [:check [:in :question-type ["translation" "multiple-choice"]]]]
   [:question :text :not nil]
   [:options :text]
   [:level :int]
   (tables/foreign-key :unit-id :units :id)))

(def answers
  (tables/create-table-sql
   :answers
   (tables/table-id)
   [:text :text]
   [:exercise-id :int]
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

(comment
  (require '[honey.sql :as sql])

  (sql/format users)
  (sql/format devices)
  (sql/format user-types)
  (sql/format user-assigned-types)
  (sql/format vocab)
  (sql/format courses)
  (sql/format user-courses)
  (sql/format units)
  (sql/format exercises)
  (sql/format answers)
  (sql/format practice-sessions)
  (sql/format exercises-completed)

  ())
