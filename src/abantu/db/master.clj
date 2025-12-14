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
   [:type :text [:check [:in :type ["creator" "distributor" "admin"]]]]
   [:email-verified :integer [:default 0]]
   [:onboarded :integer [:default 0]]
   [:address :text]
   [:mobile :text]
   [:profile-image :text]
   [:business-id :int]
   (tables/foreign-key :business-id :businesses :id)))

(def sectors
  (tables/create-table-sql
   :sectors
   (tables/table-id)
   [:name :text]))

(def categories
  (tables/create-table-sql
   :categories
   (tables/table-id)
   [:name :text]))

(def content-types
  (tables/create-table-sql
   :content-types
   (tables/table-id)
   [:name :text]))

(def cadences
  (tables/create-table-sql
   :cadences
   (tables/table-id)
   [:label :text]
   [:days :integer]))

(def baselines
  (tables/create-table-sql
   :baselines
   (tables/table-id)
   [:label :text]
   [:min :integer]
   [:max :integer]))

(def bundles
  (tables/create-table-sql
   :bundles
   (tables/table-id)
   [:name :text :not nil]
   [:uuid :text :not nil :unique]
   [:user-id :integer]
   [:video :integer :not nil [:default 0]]
   [:podcast :integer :not nil [:default 0]]
   [:blog :integer :not nil [:default 0]]
   [:hash :text]
   [:content-type-id :integer :not nil]
   [:ts-and-cs :integer]
   (tables/foreign-key :content-type-id :content-types :id)
   (tables/foreign-key :user-id :users :id)))

(def bundle-content-types
  (tables/create-table-sql
   :bundle-content-types
   (tables/table-id)
   [:bundle-id :integer :not nil]
   [:content-type-id :integer :not nil]
   (tables/foreign-key :bundle-id :bundles :id)
   (tables/foreign-key :content-type-id :content-types :id)))

(def filtered-feeds
  (tables/create-table-sql
    :filtered-feeds
    (tables/table-id)
    [:feed-id :integer :not nil]
    [:bundle-id :integer :not nil]
    (tables/foreign-key :feed-id :feeds :id)
    (tables/foreign-key :bundle-id :bundles :id)))

(def filtered-posts
  (tables/create-table-sql
    :filtered-posts
    (tables/table-id)
    [:post-id :integer :not nil]
    [:bundle-id :integer :not nil]
    (tables/foreign-key :post-id :incoming-posts :id)
    (tables/foreign-key :bundle-id :bundles :id)))

(def feeds
  (tables/create-table-sql
   :feeds
   (tables/table-id)
   [:title :text :not nil]
   [:display-picture :text]
   [:url :text]
   [:rss-url :text :not nil]
   [:user-id :integer :not nil]
   [:provider-id :integer]
   [:created-at :datetime :not nil]
   [:updated-at :datetime]
   [:content-type-id :integer :not nil]
   [:cadence-id :integer :not nil]
   [:baseline-id :integer :not nil]
   [:ts-and-cs :integer]
   [:state :text [:check [:in :state ["live" "not live" "pending"]]]]
   (tables/foreign-key :user-id :users :id)
   (tables/foreign-key :provider-id :providers :id)
   (tables/foreign-key :cadence-id :cadences :id)
   (tables/foreign-key :baseline-id :baselines :id)
   (tables/foreign-key :content-type-id :content-types :id)))

(def feed-categories
  (tables/create-table-sql
   :feed-categories
   (tables/table-id)
   [:feed-id :integer :not nil]
   [:category-id :integer :not nil]
   (tables/foreign-key :feed-id :feeds :id)
   (tables/foreign-key :category-id :categories :id)
   [[:unique [:composite :feed-id :category-id]]]))

(def providers
  (tables/create-table-sql
   :providers
   (tables/table-id)
   [:name :text]
   [:domain :text]
   [:content-type-id :integer]
   (tables/foreign-key :content-type-id :content-types :id)))

(def businesses
  (tables/create-table-sql
   :businesses
   (tables/table-id)
   [:name :text]
   [:address :text [:default nil]]
   [:url :text [:default nil]]
   [:linkedin :text [:default nil]]
   [:twitter :text [:default nil]]))

(def user-sectors
  (tables/create-table-sql
   :user-sectors
   (tables/table-id)
   [:user-id :integer :not nil]
   [:sector-id :integer :not nil]
   (tables/foreign-key :user-id :users :id)
   (tables/foreign-key :sector-id :sectors :id)))

(def feed-sectors
  (tables/create-table-sql
   :feed-sectors
   (tables/table-id)
   [:feed-id :integer :not nil]
   [:sector-id :integer :not nil]
   (tables/foreign-key :feed-id :feeds :id)
   (tables/foreign-key :sector-id :sectors :id)))

(def selection-schemas
  (tables/create-table-sql
   :selection-schemas
   (tables/table-id)
   [:output-schema-id :integer :not nil]
   [:provider-id :integer :not nil]
   (tables/foreign-key :provider-id :providers :id)))

(def incoming-posts
  (tables/create-table-sql
   :incoming-posts
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
   [:redacted :integer]
   [:posted-at :datetime]
   (tables/foreign-key :feed-id :feeds :id)
   (tables/foreign-key :creator-id :users :id)
   (tables/foreign-key :content-type-id :content-types :id)))

(def jobs
  (tables/create-table-sql
   :jobs
   (tables/table-id)
   [:job-id :text :not nil]
   [:status :text [:check [:in :status ["running" "stopped"]]]]
   [:args :text]
   [:handler :text :not nil]
   [:last-heartbeat :datetime]
   [:job-metadata-id :integer :not nil]
   (tables/foreign-key :job-metadata-id :job-metadata :id)
   [[:foreign-key :job-metadata-id] [:references :job-metadata :id] :on-delete :cascade]))

(def job-metadata
  (tables/create-table-sql
   :job-metadata
   (tables/table-id)
   [:initial-delay :integer]
   [:auto-start :integer]
   [:stop-after-fail :integer]
   [:kill-after :integer]
   [:num-calls :integer]
   [:interval :integer]
   [:recurring :integer]
   [:created-at :datetime]
   [:sleep :integer]))

(def events
  (tables/create-table-sql
   :events
   (tables/table-id)
   [:timestamp :datetime]
   [:event :text [:check [:in :event ["impression" "click" "view"]]]]
   [:feed-id :integer :not nil]
   [:post-id :integer]
   [:content-type-id :not nil]
   [:creator-id :not nil]
   [:bundle-id :integer :not nil]
   [:distributor-id :integer :not nil]
   (tables/foreign-key :feed-id :feeds :id)
   (tables/foreign-key :post-id :incoming-posts :id)
   (tables/foreign-key :content-type-id :content-types :id)
   (tables/foreign-key :creator-id :users :id)
   (tables/foreign-key :bundle-id :bundles :id)
   (tables/foreign-key :distributor-id :users :id)))

(def event-categories
  (tables/create-table-sql
   :event-categories
   (tables/table-id)
   [:event-id :integer :not nil]
   [:category-id :integer :not nil]
   (tables/foreign-key :event-id :events :id)
   (tables/foreign-key :category-id :categories :id)))

(comment
  (require '[honey.sql :as sql])

  (sql/format users)
  (sql/format sectors)
  (sql/format content-types)
  (sql/format cadences)
  (sql/format categories)
  (sql/format baselines)
  (sql/format bundles)
  (sql/format bundle-content-types)
  (sql/format feeds)
  (sql/format feed-categories)
  (sql/format providers)
  (sql/format businesses)
  (sql/format user-sectors)
  (sql/format feed-sectors)
  (sql/format selection-schemas)
  (sql/format incoming-posts)
  (sql/format jobs)
  (sql/format job-metadata)

  (sql/format events)
  (sql/format event-categories)
  ())
