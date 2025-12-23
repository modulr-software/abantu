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


(comment
  (require '[honey.sql :as sql])

  (sql/format users)

  ())
