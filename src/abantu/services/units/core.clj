(ns abantu.services.units.core
  (:require [abantu.db.interface :as db]
            [abantu.services.exercises.interface :as exercise]
            [abantu.services.units.update :as update]
            [honey.sql.helpers :as h]
            [abantu.util :as util]))

(defn- attach-exercises [ds unit]
  (->> (exercise/find (exercise/use-query ds) {:unit-id (:id unit)})
       (assoc unit :exercises)))

(defn -lookup  [ds {:keys [id uuid]}]
  (->> (db/find ds (cond-> {:tname :units
                            :ret :1}
                     (some? uuid) (h/where [:= :uuid uuid])
                     (some? id) (h/where [:= :id id])))
       (attach-exercises ds)))

(defn -all [ds]
  (->> (db/find ds {:tname :units
                    :ret :*})
       (mapv #(attach-exercises ds %))))

(defn -find [ds {:keys [id uuid course-id]}]
  (->> (db/find ds (cond-> {:tname :units
                            :ret :*}
                     (some? uuid) (h/where [:= :uuid uuid])
                     (some? id) (h/where [:= :id id])
                     (some? course-id) (h/where [:= :course-id course-id])))
       (mapv #(attach-exercises ds %))))

(defn -create [ds {:keys [uuid exercises] :as update}]
  (let [{:keys [id]} (db/insert! ds {:tname :units
                                     :values (-> (dissoc update :exercises)
                                                 (assoc :uuid (or uuid (util/uuid))))
                                     :ret :1})
        exercises' (mapv #(assoc % :unit-id id) exercises)]
    (run! #(exercise/create (exercise/use-mutation ds) %) exercises')
    (update/apply (-lookup ds {:id id}) {:type :create
                                         :payload update})))

(defn -delete [ds {:keys [id uuid] :as update}]
  (let [{:keys [id]} (-lookup ds {:id id :uuid uuid})
        exmut (exercise/use-mutation ds)
        exercise-ids (->> (db/find ds {:tname :exercises
                                       :where [:= :unit-id id]})
                          (mapv :id))]
    (run! #(exercise/delete exmut {:id %}) exercise-ids)
    (db/delete! ds {:tname :practice-sessions
                    :where [:= :unit-id id]})
    (update/apply nil {:type :delete
                       :payload update})))

(defn -set-name [ds {:keys [id uuid name] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:name name}
                    :where [:= :id (:id unit)]})
    (update/apply unit {:type :set-name
                        :payload update})))

(defn -set-description [ds {:keys [id uuid description] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:description description}
                    :where [:= :id (:id unit)]})
    (update/apply unit {:type :set-description
                        :payload update})))

(defn -set-level [ds {:keys [id uuid level] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:level level}
                    :where [:= :id (:id unit)]})
    (update/apply unit {:type :set-level
                        :payload update})))

(defn -set-type [ds {:keys [id uuid type] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:type type}
                    :where [:= :id (:id unit)]})
    (update/apply unit {:type :set-type
                        :payload update})))

(defn -set-course-id [ds {:keys [id uuid course-id] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:course-id course-id}
                    :where [:= :id (:id unit)]})
    (update/apply unit {:type :set-course-id
                        :payload update})))

(defn -set-position [ds {:keys [id uuid position] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:position position}
                    :where [:= :id (:id unit)]})
    (update/apply unit {:type :set-position
                        :payload update})))

(comment
  (def ds (db/ds :master))

  (-lookup ds {:id 1})
  (-find ds {:id 1})
  (-all ds)
  :end)
