(ns abantu.services.versions.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.services.versions.core :as core]
            [abantu.services.changes.interface :as changes]
            [abantu.util :as util]))

(def ?Version
  [:map
   [:id :int]
   [:timestamp :string]
   [:label {:optional true} (util/maybe :string)]
   [:applied :boolean]
   [:course-id :int]
   [:course-changes [:vector changes/?CourseChange]]
   [:unit-changes [:vector changes/?UnitChange]]
   [:exercise-changes [:vector changes/?ExerciseChange]]])

(def ?Opts
  [:map [:with-changes? :boolean]])

(def ?Lookup
  [:or
   (mu/merge [:map [:id :int]] ?Opts)
   (mu/merge [:map [:timestamp :string]] ?Opts)])

(def ?Find
  [:or
   (mu/merge [:map [:course-id :int]] ?Opts)
   (mu/merge [:map [:label :string]] ?Opts)])

(def ?AddVersion
  (mu/select-keys ?Version [:label :course-id]))

(def ?SetVersionLabel
  (mu/merge (mu/select-keys ?Version [:id :label]) ?Opts))

(malt/defprotocol VersionsQuery
  (lookup [input ?Lookup]
    (util/maybe ?Version))
  (find [input ?Find]
    [:vector ?Version])
  (all [input ?Opts]
    [:vector ?Version]))

(malt/defprotocol VersionsMutation
  (create! [input ?AddVersion]
    (util/maybe ?Version))
  (set-version-label! [input ?SetVersionLabel]
    (util/maybe ?Version)))

(defn use-query [ds]
  (reify VersionsQuery
    (lookup [_ input]
      (core/-lookup ds input))
    (find [_ input]
      (core/-find ds input))
    (all [_ input]
      (core/-all ds input))))

(defn use-mutation [ds]
  (reify VersionsMutation
    (create! [_ input]
      (core/-create! ds input))
    (set-version-label! [_ input]
      (core/-set-version-label! ds input))))

(comment

  (require '[abantu.db.util :as db.util])
  (def ds (db.util/conn :student 1))

  (def vcq (use-query ds))
  (def vcm (use-mutation ds))

  ;; queries

  (lookup vcq {:id 1})
  (lookup vcq {:id 1 :with-changes? true})
  (lookup vcq {:timestamp "2025-01-01T00:00:00Z"
               :with-changes? true})

  (find vcq {:course-id 1})
  (find vcq {:course-id 1 :with-changes? true})
  (find vcq {:label "draft 1"
             :with-changes? true})

  (all vcq {:with-changes? false})
  (all vcq {:with-changes? true})

  ;; mutations

  (create! vcm {:label "draft 1"
                :course-id 1})
  (create! vcm {:course-id 1})

  (set-version-label! vcm {:id 1
                           :label "review draft"})
  (set-version-label! vcm {:id 1
                           :label "review draft"
                           :with-changes? true})

  :end)
