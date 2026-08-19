(ns abantu.services.exercises.core
  (:require [abantu.db.interface :as db]
            [honey.sql.helpers :as honey]))

(let [id nil
      unit-id 1
      course-id 2]
  )

(honey/where  {})

(defn -lookup  [ds {:keys [id unit-id course-id]}]
  (db/find ds (cond-> {:tname :users
                       :ret :*}
                (some? id) (honey/where [:= :id id])
                (some? unit-id) (honey/where [:= :unit-id unit-id])
                (some? course-id) (honey/where [:= :course-id course-id]))))

(defn -all [ds ]
  (db/find ds {:tname :users
               :ret :*}))

(defn -find [ds {:keys [id unit-id course-id]}]
  (db/find ds (cond-> {:tname :users
                       :ret :*}
                (some? id) (honey/where [:= :id id])
                (some? unit-id) (honey/where [:= :unit-id unit-id])
                (some? course-id) (honey/where [:= :course-id course-id]))))

(comment
  (-lookup (db/ds :master) {:id 1})
  (-find (db/ds :master) {:id 1})
  (-all (db/ds :master))
  :end)

(defmulti apply-update (fn [_ action] (:type action)))

(defmethod apply-update :set-unit [exercise {:keys [payload]}]
  (assoc exercise :unit-id (:unit-id payload)))

(defmethod apply-update :set-instruction [exercise {:keys [payload]}]
  (assoc exercise :instruction (:instruction payload)))

(defmethod apply-update :set-question-content [exercise {:keys [payload]}]
  (assoc exercise :question-content (:question-content payload)))

(defn- exists? [ds id]
  (when-let [exercise (-lookup ds {:id id})] 
    exercise))

(defn -set-unit [ds {:keys [id unit-id] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:unit-id unit-id}})
    (apply-update exercise {:type :set-unit
                            :payload update})))

(defn -set-instruction [ds {:keys [id instruction] :as update}]
  (when-let [exercise (exists? ds id)] 
    (db/update! ds {:tname :exercises
                    :values {:instruction instruction}})
    (assoc apply-update exercise {:type :set-instruction
                                  :payload update})))

(defn -set-question-content [ds {:keys [id question-content] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:question-content question-content}})
    (assoc apply-update exercise {:type :set-question-content
                                  :payload update})))
