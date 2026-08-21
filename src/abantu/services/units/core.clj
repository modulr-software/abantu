(ns abantu.services.units.core
  (:require [abantu.db.interface :as db]
            [abantu.services.exercises.core :as exercises]
            [abantu.services.units.update :as update]
            [honey.sql.helpers :as h]))

;;TODO: update all usages of the exercise impl functions to use the interface instead

(defn- attach-exercises [ds unit]
  (->> (exercises/-find ds {:unit-id (:id unit)})
       (assoc unit :exercises)))

(defn -lookup  [ds {:keys [id]}]
  (->> (db/find ds (cond-> {:tname :units
                            :ret :1}
                     (some? id) (h/where [:= :id id])))
       (attach-exercises ds)))

(defn -all [ds]
  (->> (db/find ds {:tname :units
                    :ret :*})
       (mapv #(attach-exercises ds %))))

(defn -find [ds {:keys [id course-id]}]
  (->> (db/find ds (cond-> {:tname :units
                            :ret :*}
                     (some? id) (h/where [:= :id id])
                     (some? course-id) (h/where [:= :course-id course-id])))
       (mapv #(attach-exercises ds %))))

(defn- exists? [ds id]
  (when-let [unit (-lookup ds {:id id})]
    unit))

(defn -create [ds {:keys [exercises] :as update}]
  (let [{:keys [id]} (db/insert! ds {:tname :units
                                     :values (dissoc update :exercises) 
                                     :ret :1})
        exercises' (mapv #(assoc % :unit-id id) exercises)]
    (run! #(exercises/-create ds %) exercises')
    (update/apply (-lookup ds {:id id}) {:type :create
                                         :payload update})))

;; TODO: add remove impl here

(defn -set-name [ds {:keys [id name] :as update}]
  (when-let [unit (exists? ds id)]
    (db/update! ds {:tname :units
                    :values {:name name}})
    (update/apply unit {:type :set-name
                        :payload update})))

(defn -set-description [ds {:keys [id description] :as update}]
  (when-let [unit (exists? ds id)]
    (db/update! ds {:tname :units
                    :values {:description description}})
    (update/apply unit {:type :set-description
                        :payload update})))

(defn -set-level [ds {:keys [id level] :as update}]
  (when-let [unit (exists? ds id)]
    (db/update! ds {:tname :units
                    :values {:level level}})
    (update/apply unit {:type :set-level
                        :payload update})))

(defn -set-type [ds {:keys [id type] :as update}]
  (when-let [unit (exists? ds id)]
    (db/update! ds {:tname :units
                    :values {:type type}})
    (update/apply unit {:type :set-type
                        :payload update})))

(defn -set-course-id [ds {:keys [id course-id] :as update}]
  (when-let [unit (exists? ds id)]
    (db/update! ds {:tname :units
                    :values {:course-id course-id}})
    (update/apply unit {:type :set-course-id
                        :payload update})))

(defn -set-position [ds {:keys [id position] :as update}]
  (when-let [unit (exists? ds id)]
    (db/update! ds {:tname :units
                    :values {:position position}})
    (update/apply unit {:type :set-position
                        :payload update})))

(comment
  (def ds (db/ds :master))

  (-lookup ds {:id 1})
  (-find ds {:id 1})
  (-all ds)
  :end)
