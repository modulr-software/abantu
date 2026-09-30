(ns abantu.services.units.core
  (:require [abantu.db.interface :as db]
            [abantu.services.changes.core :as changes]
            [abantu.services.exercises.interface :as exercise]
            [abantu.services.units.update :as update]
            [honey.sql.helpers :as h]
            [abantu.util :as util]))

(defn- attach-exercises [ds unit]
  (->> (exercise/find (exercise/use-query ds) {:unit-id (:id unit)})
       (assoc unit :exercises)))

(defn- add-change! [changes-api ds unit change-type payload exercises]
  (when changes-api
    (changes/-add-unit-update!
     ds
     {:change-type change-type
      :update (merge payload
                     {:uuid (:uuid unit)
                      :course-uuid (:uuid (db/find-one ds {:tname :courses
                                                           :where [:= :id (:course-id unit)]}))}
                     (when (seq exercises) {:exercises exercises}))})))

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

(defn -create [changes-api ds {:keys [uuid course-id exercises] :as update}]
  (let [{:keys [id]} (db/insert! ds {:tname :units
                                     :values (-> (dissoc update :exercises)
                                                 (assoc :uuid (or uuid (util/uuid))))
                                     :ret :1})
        exercises' (mapv #(assoc % :unit-id id :course-id course-id) exercises)]
    (run! #(exercise/create (exercise/use-mutation ds) %) exercises')
    (let [applied (update/apply (-lookup ds {:id id}) {:type :create
                                                      :payload (dissoc update :exercises)})]
      ;; cascade: one unit change + one change per created exercise
      (add-change! changes-api ds applied :create (dissoc update :exercises)
                   (mapv #(dissoc % :comments) (:exercises applied)))
      applied)))

(defn -delete [changes-api ds {:keys [id uuid] :as update}]
  (let [{:keys [id] :as unit} (-lookup ds {:id id :uuid uuid})
        exmut (exercise/use-mutation ds)
        exercise-ids (->> (db/find ds {:tname :exercises
                                       :where [:= :unit-id id]})
                          (mapv :id))]
    (run! #(exercise/delete exmut {:id %}) exercise-ids)
    (db/delete! ds {:tname :practice-sessions
                    :where [:= :unit-id id]})
    (db/delete! ds {:tname :units
                    :where [:= :id id]})
    ;; cascade: one unit change + one change per deleted exercise
    (add-change! changes-api ds unit :delete nil
                 (mapv (fn [exercise] {:uuid (:uuid exercise)}) (:exercises unit)))
    (update/apply nil {:type :delete
                       :payload update})))

(defn -set-name [changes-api ds {:keys [id uuid name] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:name name}
                    :where [:= :id (:id unit)]})
    (let [applied (update/apply unit {:type :set-name
                                      :payload update})]
      (add-change! changes-api ds applied :set-name update nil)
      applied)))

(defn -set-description [changes-api ds {:keys [id uuid description] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:description description}
                    :where [:= :id (:id unit)]})
    (let [applied (update/apply unit {:type :set-description
                                      :payload update})]
      (add-change! changes-api ds applied :set-description update nil)
      applied)))

(defn -set-level [changes-api ds {:keys [id uuid level] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:level level}
                    :where [:= :id (:id unit)]})
    (let [applied (update/apply unit {:type :set-level
                                      :payload update})]
      (add-change! changes-api ds applied :set-level update nil)
      applied)))

(defn -set-type [changes-api ds {:keys [id uuid type] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:type type}
                    :where [:= :id (:id unit)]})
    (let [applied (update/apply unit {:type :set-type
                                      :payload update})]
      (add-change! changes-api ds applied :set-type update nil)
      applied)))

(defn -set-course-id [changes-api ds {:keys [id uuid course-id] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:course-id course-id}
                    :where [:= :id (:id unit)]})
    (let [applied (update/apply unit {:type :set-course-id
                                      :payload update})]
      (add-change! changes-api ds (assoc applied :course-id course-id)
                   :set-course-id update nil)
      applied)))

(defn -set-position [changes-api ds {:keys [id uuid position] :as update}]
  (when-let [unit (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :units
                    :values {:position position}
                    :where [:= :id (:id unit)]})
    (let [applied (update/apply unit {:type :set-position
                                      :payload update})]
      (add-change! changes-api ds applied :set-position update nil)
      applied)))

(comment
  (def ds (db/ds :master))

  (-lookup ds {:id 1})
  (-find ds {:id 1})
  (-all ds)
  :end)
