(ns abantu.services.units.core
  (:require [abantu.db.interface :as db]
            [abantu.services.units.update :as update]
            [honey.sql.helpers :as h]))

(defn -lookup  [ds {:keys [id]}]
  (db/find ds (cond-> {:tname :units
                       :ret :1}
                (some? id) (h/where [:= :id id]))))

(defn -all [ds]
  (db/find ds {:tname :units
               :ret :*}))

(defn -find [ds {:keys [id course-id]}]
  (db/find ds (cond-> {:tname :units
                       :ret :*}
                (some? id) (h/where [:= :id id])
                (some? course-id) (h/where [:= :course-id course-id]))))

(defn- exists? [ds id]
  (when-let [unit (-lookup ds {:id id})]
    unit))

(defn -create [ds update]
  (let [{:keys [id]} (db/insert! ds {:tname :units
                                     :values update
                                     :ret :1})]
    (update/apply (-lookup ds {:id id}) {:type :create
                                         :payload update})))

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
