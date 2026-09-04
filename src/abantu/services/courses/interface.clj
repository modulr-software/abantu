(ns abantu.services.courses.interface
  (:require [abantu.util :as util]
            [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.db.util :as db.util]
            [abantu.services.units.interface :as units]
            [abantu.services.courses.core :as courses]))

(def ?User
  [:map
   [:email (util/maybe :string)]
   [:firstname (util/maybe :string)]
   [:lastname (util/maybe :string)]
   [:email-verified :boolean]
   [:archived :boolean]
   [:approved :boolean]
   [:mobile (util/maybe :string)]
   [:profile-image (util/maybe :string)]])

(def ?Course
  [:map
   [:id :int]
   [:name :string]
   [:language :string]
   [:description (util/maybe :string)]
   [:publishable :boolean]
   [:visible :boolean]
   [:review-pending :boolean]
   [:creator (util/maybe ?User)]
   [:units [:vector units/?Unit]]])

(def ?Lookup
  (mu/select-keys ?Course [:id]))

(def ?Find
  [:map
   [:id {:optional true} :int]
   [:creator-id :int]])

(def ?Create
  (-> (mu/dissoc ?Course :id)
      (mu/dissoc :creator)
      (mu/assoc :creator-id :int)
      (mu/update-entry-properties :description assoc :optional true)
      (mu/update-entry-properties :publishable assoc :optional true)
      (mu/update-entry-properties :visible assoc :optional true)
      (mu/update-entry-properties :review-pending assoc :optional true)
      (mu/assoc :units [:vector (mu/dissoc units/?Create :course-id)])))

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
  [:map
   [:id :int]
   [:creator-id :int]])

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
  (create [input ?Create]
    (util/maybe ?Course))
  (delete [input ?Lookup]
    :nil)
  (set-name [input ?SetName]
    (util/maybe ?Course))
  (set-language [input ?SetLanguage]
    (util/maybe ?Course))
  (set-description [input ?SetDescription]
    (util/maybe ?Course))
  (set-publishable [input ?SetPublishable]
    (util/maybe ?Course))
  (set-visible [input ?SetVisible]
    (util/maybe ?Course))
  (set-review-pending [input ?SetReviewPending]
    (util/maybe ?Course))
  (set-creator-id [input ?SetCreatorId]
    (util/maybe ?Course)))

(defn use-mutation
  ([] (use-mutation (db.util/conn)))
  ([ds]
   (malt/reify CourseMutation
     (create [_ input]
       (courses/-create ds input))
     (delete [_ input]
       (courses/-delete ds input))
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

(comment

  (def cm (use-mutation))

  (create cm {:name "zulu basics"
              :language "zulu"
              :description "learn zulu"
              :creator-id 1
              :units []})

  (set-name cm {:id 1
                :name "afrikaans course 2"})
  (set-language cm {:id 1
                    :language "english"})
  (set-description cm {:id 1
                       :description "learn about pronouns and more"})
  (set-publishable cm {:id 1
                       :publishable true})
  (set-visible cm {:id 1
                   :visible true})
  (set-review-pending cm {:id 1
                          :review-pending true})
  (set-creator-id cm {:id 1
                      :creator-id 2})

  :end)
