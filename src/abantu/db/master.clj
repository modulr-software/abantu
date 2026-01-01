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
   [:onboarded :integer [:default 0]]
   [:mobile :text]
   [:profile-image :text]))

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

(def units 
  (tables/create-table-sql
    :units
    (tables/table-id)
    [:name :text :not nil]
    [:description :text]
    [:creator-id :integer]
    (tables/foreign-key :creator-id :users :id)))

(def exercises
  (tables/create-table-sql
   :exercises
   (tables/table-id)
   [:unit-id :integer :not nil]
   [:question-type :text [:check [:in :question-type ["translation" "multiple-choice"]]]]
   [:question :text :not nil]
   [:options :text]
   (tables/foreign-key :unit-id :units :id)))

(def answers
  (tables/create-table-sql
   :answers
   (tables/table-id)
   [:text :text]
   [:exercise-id :int]
   (tables/foreign-key :exercise-id :exercises :id)))

(comment
  (require '[honey.sql :as sql])

  (sql/format users)

  ())
