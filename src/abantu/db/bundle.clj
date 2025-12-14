(ns abantu.db.bundle
  (:require [abantu.db.tables :as tables]
            [honey.sql :as sql]))

(def event-categories
  (tables/create-table-sql
   :event-categories
   (tables/table-id)
   [:event-id :integer :not nil]
   [:category-id :text :not nil]
   (tables/foreign-key :event-id :analytics :id)))

(def outgoing-posts
  (tables/create-table-sql
   :outgoing-posts
   (tables/table-id)
   [:post-id :text :not nil]
   [:feed-id :integer :not nil]
   [:title :text :not nil]
   [:thumbnail :text]
   [:info :text]
   [:url :text]
   [:stream-url :text]
   [:creator-id :integer :not nil]
   [:season :integer]
   [:episode :integer]
   [:content-type-id :integer :not nil]
   [:posted-at :datetime]
   (tables/foreign-key :feed-id :feeds :id)
   (tables/foreign-key :creator-id :users :id)
   (tables/foreign-key :content-type-id :content-types :id)))

(def bundle-categories
  (tables/create-table-sql
   :bundle-categories
   (tables/table-id)
   [:bundle-id :int :not nil]
   [:category-id :int :not nil]
   (tables/foreign-key :bundle-id :bundles :id)
   (tables/foreign-key :category-id :categories :id)))

(def post-heuristics
  (tables/create-table-sql
   :post-heuristics
   (tables/table-id)
   [:post-id :integer :not nil]
   [:long-heuristic :integer]
   [:short-heuristic :integer]
   [[:unique [:composite :post-id]]]))

(def analytics
  (tables/create-table-sql
   :analytics
   (tables/table-id)
   [:post-id :integer :not nil]
   [:event-type :text :not nil]
   [:timestamp :text :not nil]))

(comment
  (sql/format event-categories)
  (sql/format outgoing-posts)
  (sql/format bundle-categories)
  (sql/format post-heuristics)
  (sql/format analytics)
  ())

