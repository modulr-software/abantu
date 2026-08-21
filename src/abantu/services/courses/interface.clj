(ns abantu.services.courses.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.db.util :as db.util]
            [abantu.services.courses.core :as courses]))

(defn- maybe [?schema]
  [:or ?schema :nil])

(def ?Course
  [:map
   [:id :int]
   [:name :string]
   [:language :string]
   [:description :string]
   [:publishable :int]
   [:visible :int]
   [:review-pending :int]
   [:creator-id :int]])

(def ?Lookup
  (mu/select-keys ?Course [:id]))

(def ?Find
  (mu/select-keys ?Course [:id :creator-id]))

(def ?SetName
  (mu/select-keys ?Course [:id :name]))

(def ?SetLanguage
  (mu/select-keys ?Course [:id :language]))

(def ?SetDescription
  (mu/select-keys ?Course [:id :description]))

(def ?SetPublishable
  (mu/select-keys ?Course [:id :publishable]))

(def ?SetVisible
  (mu/select-keys ?Course [:id :visible]))

(def ?SetReviewPending
  (mu/select-keys ?Course [:id :review-pending]))

(def ?SetCreatorId
  (mu/select-keys ?Course [:id :creator-id]))

(malt/defprotocol CourseQuery
  (lookup [input ?Lookup] ?Course)
  (find [input ?Find] [:vector ?Course])
  (all [] [:vector ?Course]))

(defn use-query
  ([] (use-query (db.util/conn)))
  ([ds]
   (malt/reify CourseQuery
     (lookup [_ input]
       (courses/-lookup ds input))
     (find [_ input]
       (courses/-find ds input))
     (all [_]
       (courses/-all ds)))))

(malt/defprotocol CourseMutation
  (create [input (mu/dissoc ?Course :id)]
    (maybe ?Course))
  (set-name [input ?SetName]
    (maybe ?Course))
  (set-language [input ?SetLanguage]
    (maybe ?Course))
  (set-description [input ?SetDescription]
    (maybe ?Course))
  (set-publishable [input ?SetPublishable]
    (maybe ?Course))
  (set-visible [input ?SetVisible]
    (maybe ?Course))
  (set-review-pending [input ?SetReviewPending]
    (maybe ?Course))
  (set-creator-id [input ?SetCreatorId]
    (maybe ?Course)))

(defn use-mutation
  ([] (use-mutation (db.util/conn)))
  ([ds]
   (malt/reify CourseMutation
     (create [_ input]
       (courses/-create ds input))
     (set-name [_ input]
       (courses/-set-name ds input))
     (set-language [_ input]
       (courses/-set-language ds input))
     (set-description [_ input]
       (courses/-set-description ds input))
     (set-publishable [_ input]
       (courses/-set-publishable ds input))
     (set-visible [_ input]
       (courses/-set-visible ds input))
     (set-review-pending [_ input]
       (courses/-set-review-pending ds input))
     (set-creator-id [_ input]
       (courses/-set-creator-id ds input)))))
