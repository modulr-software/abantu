(ns abantu.services.units.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.db.util :as db.util]
            [abantu.services.exercises.interface :as exercises]
            [abantu.services.units.core :as units]))

(defn- maybe [?schema]
  [:or ?schema :nil])

(def ?Unit
  [:map
   [:id :int]
   [:course-id :int]
   [:name :string]
   [:description :string]
   [:level :int]
   [:type :string]
   [:position :int]
   [:exercises [:vector exercises/?Exercise]]])

(def ?Lookup
  (mu/select-keys ?Unit [:id]))

(def ?Find
  (mu/select-keys ?Unit [:id :course-id]))

(def ?Create
  (-> (mu/dissoc ?Unit :id)
      (mu/assoc :exercises [:vector (mu/dissoc exercises/?Create :unit-id)])))

(def ?SetCourseId
  (mu/select-keys ?Unit [:id :course-id]))

(def ?SetName
  (mu/select-keys ?Unit [:id :name]))

(def ?SetDescription
  (mu/select-keys ?Unit [:id :description]))

(def ?SetLevel
  (mu/select-keys ?Unit [:id :level]))

(def ?SetType
  (mu/select-keys ?Unit [:id :type]))

(def ?SetPosition
  (mu/select-keys ?Unit [:id :position]))

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
    (maybe ?Unit))
  (set-name [input ?SetName]
    (maybe ?Unit))
  (set-description [input ?SetDescription]
    (maybe ?Unit))
  (set-level [input ?SetLevel]
    (maybe ?Unit))
  (set-type [input ?SetType]
    (maybe ?Unit))
  (set-course-id [input ?SetCourseId]
    (maybe ?Unit))
  (set-position [input ?SetPosition]
    (maybe ?Unit)))

(defn use-mutation
  ([] (use-mutation (db.util/conn)))
  ([ds]
   (malt/reify UnitMutation
     (create [_ input]
       (units/-create ds input))
     ;;TODO: add remove here
     (set-name [_ input]
       (units/-set-name ds input))
     (set-description [_ input]
       (units/-set-description ds input))
     (set-level [_ input]
       (units/-set-level ds input))
     (set-type [_ input]
       (units/-set-type ds input))
     (set-course-id [_ input]
       (units/-set-course-id ds input))
     (set-position [_ input]
       (units/-set-position ds input)))))
