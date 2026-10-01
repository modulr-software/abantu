(ns abantu.services.units.interface
  (:require [abantu.util :as util]
            [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.db.util :as db.util]
            [abantu.services.exercises.interface :as exercises]
            [abantu.services.units.core :as units]))

(def ?Unit
  [:map
   [:id :int]
   [:uuid (util/maybe :string)]
   [:course-id :int]
   [:name :string]
   [:description (util/maybe :string)]
   [:level (util/maybe :int)]
   [:type [:enum "lesson" "practice"]]
   [:position (util/maybe :int)]
   [:exercises [:vector exercises/?Exercise]]])

(def ?Lookup
  [:or
   [:map [:id :int]]
   [:map [:uuid :string]]])

(def ?Find
  [:map
   [:course-id :int]])

(def ?Create
  (-> (mu/dissoc ?Unit :id)
      (mu/dissoc :creator)
      (mu/update-entry-properties :uuid assoc :optional true)
      (mu/update-entry-properties :description assoc :optional true)
      (mu/update-entry-properties :level assoc :optional true)
      (mu/update-entry-properties :position assoc :optional true)
      (mu/assoc :exercises [:vector (-> (mu/dissoc exercises/?Create :unit-id)
                                        (mu/dissoc :course-id))])))

(def ?SetCourseId
  [:or (mu/select-keys ?Unit [:id :course-id])
   (mu/select-keys ?Unit [:uuid :course-id])])

(def ?SetName
  [:or (mu/select-keys ?Unit [:id :name])
   (mu/select-keys ?Unit [:uuid :name])])

(def ?SetDescription
  [:or (mu/select-keys ?Unit [:id :description])
   (mu/select-keys ?Unit [:uuid :description])])

(def ?SetLevel
  [:or (mu/select-keys ?Unit [:id :level])
   (mu/select-keys ?Unit [:uuid :level])])

(def ?SetType
  [:or (mu/select-keys ?Unit [:id :type])
   (mu/select-keys ?Unit [:uuid :type])])

(def ?SetPosition
  [:or (mu/select-keys ?Unit [:id :position])
   (mu/select-keys ?Unit [:uuid :position])])

(malt/defprotocol UnitQuery
  (lookup [input ?Lookup] ?Unit)
  (find [input ?Find] [:vector ?Unit])
  (all [] [:vector ?Unit]))

(defn use-query
  ([] (use-query (db.util/conn)))
  ([ds]
   (malt/reify UnitQuery
     (lookup [_ input]
       (units/-lookup ds input))
     (find [_ input]
       (units/-find ds input))
     (all [_]
       (units/-all ds)))))

(malt/defprotocol UnitMutation
  (create [input ?Create]
    (util/maybe ?Unit))
  (delete [input ?Lookup]
    :nil)
  (set-name [input ?SetName]
    (util/maybe ?Unit))
  (set-description [input ?SetDescription]
    (util/maybe ?Unit))
  (set-level [input ?SetLevel]
    (util/maybe ?Unit))
  (set-type [input ?SetType]
    (util/maybe ?Unit))
  (set-course-id [input ?SetCourseId]
    (util/maybe ?Unit))
  (set-position [input ?SetPosition]
    (util/maybe ?Unit)))

(defn use-mutation
  "Pass {:changes-api an-api} to record a unit change for every mutation.
   Without one, mutations change the unit but record nothing - which is what a
   cascaded create wants, since the caller's single change already covers the
   whole tree."
  ([] (use-mutation (db.util/conn)))
  ([ds] (use-mutation ds {}))
  ([ds opts]
   (malt/reify UnitMutation
     (create [_ input]
       (units/-create ds opts input))
     (delete [_ input]
       (units/-delete ds opts input))
     (set-name [_ input]
       (units/-set-name ds opts input))
     (set-description [_ input]
       (units/-set-description ds opts input))
     (set-level [_ input]
       (units/-set-level ds opts input))
     (set-type [_ input]
       (units/-set-type ds opts input))
     (set-course-id [_ input]
       (units/-set-course-id ds opts input))
     (set-position [_ input]
       (units/-set-position ds opts input)))))

(comment

  (def um (use-mutation (db.util/conn :student 1)))

  (create um {:name "pronouns 1"
              :course-id 1
              :description "useful stuff"
              :type "lesson"
              :exercises []})

  (set-name um {:id 1
                :name "pronouns 1"})
  (set-description um {:id 1
                       :description "even more useful stuff"})
  (set-level um {:id 1
                 :level 2})
  (set-type um {:id 1
                :type "practice"})
  (set-course-id um {:id 1
                     :course-id 1})
  (set-position um {:id 1
                    :position 2})

  :end)
