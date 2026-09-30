(ns abantu.services.courses.core
  (:require [abantu.db.interface :as db]
            [abantu.services.changes.core :as changes]
            [abantu.services.units.interface :as unit]
            [abantu.services.courses.update :as update]
            [honey.sql.helpers :as h]
            [abantu.services.users :as users]
            [abantu.util :as util]))

(defn- attach-units [ds course]
  (->> (unit/find (unit/use-query ds)
                  {:course-id (:id course)})
       (assoc course :units)))

(defn- attach-creator [ds {:keys [creator-id] :as course}]
  (if creator-id
    (let [creator (users/get-user ds creator-id)]
      (-> (assoc course :creator (or creator nil))
          (dissoc :creator-id)))
    (-> (assoc course :creator nil)
        (dissoc :creator-id))))

(defn- process-bools [course]
  (util/parse-bool-keys course [:publishable :visible :review-pending]))

;; a create cascade replays the whole tree, minus comments (never replayable)
(defn- cascaded-unit [unit]
  (assoc unit :exercises (mapv #(dissoc % :comments) (:exercises unit))))

;; a delete cascade carries identity only
(defn- deleted-unit [unit]
  {:uuid (:uuid unit)
   :exercises (mapv (fn [exercise] {:uuid (:uuid exercise)}) (:exercises unit))})

(defn- add-change! [changes-api ds course change-type payload units]
  (when changes-api
    (changes/-add-course-update!
     ds
     {:change-type change-type
      :update (merge payload
                     {:uuid (:uuid course)}
                     (when (seq units) {:units units}))})))

(defn -lookup [ds {:keys [id uuid]}]
  (->> (db/find-one ds (cond-> {:tname :courses}
                         (some? uuid) (h/where [:= :uuid uuid])
                         (some? id) (h/where [:= :id id])))
       (process-bools)
       (attach-units ds)
       (attach-creator ds)))

(defn -all [ds]
  (->> (db/find ds {:tname :courses
                    :ret :*})
       (mapv (comp process-bools
                   (partial attach-units ds)
                   (partial attach-creator ds)))))

(defn -find [ds {:keys [id uuid creator-id]}]
  (->> (db/find ds (cond-> {:tname :courses
                            :ret :*}
                     (some? uuid) (h/where [:= :uuid uuid])
                     (some? id) (h/where [:= :id id])
                     (some? creator-id) (h/where [:= :creator-id creator-id])))
       (mapv
        (comp process-bools
              (partial attach-units ds)
              (partial attach-creator ds)))))

(defn -create [changes-api ds {:keys [uuid units] :as update}]
  (let [{:keys [id]} (db/insert! ds {:tname :courses
                                     :values (-> (dissoc update :units)
                                                 (assoc :uuid (or uuid (util/uuid))))
                                     :ret :1})
        units' (mapv #(assoc % :course-id id) units)]
    (run! #(unit/create (unit/use-mutation ds) %) units')
    (let [applied (update/apply (-lookup ds {:id id}) {:type :create
                                                       :payload (dissoc update :units)})]
      ;; cascade: one course change + one change per unit + one per exercise
      (add-change! changes-api ds applied :create (dissoc update :units)
                   (mapv cascaded-unit (:units applied)))
      applied)))

(defn -delete [changes-api ds {:keys [id uuid] :as update}]
  (let [{:keys [id] :as course} (-lookup ds {:id id :uuid uuid})
        unmut (unit/use-mutation ds)
        unit-ids (db/find ds {:tname :units
                              :where [:= :course-id id]})]
    (run! #(unit/delete unmut %) unit-ids)
    (try
      (db/delete! ds {:tname :user-courses
                      :where [:= :course-id id]})
      (db/delete! ds {:tname :course-editors
                      :where [:= :course-id id]})
      (catch Exception _))
    (db/delete! ds {:tname :courses
                    :where [:= :id id]})
    ;; cascade: one course change + one change per unit + one per exercise
    (add-change! changes-api ds course :delete nil
                 (mapv deleted-unit (:units course)))
    (update/apply nil {:type :delete
                       :payload update})))

(defn -set-name [changes-api ds {:keys [id uuid name] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:name name}
                    :where [:= :id (:id course)]})
    (let [applied (update/apply course {:type :set-name
                                        :payload update})]
      (add-change! changes-api ds applied :set-name update nil)
      applied)))

(defn -set-language [changes-api ds {:keys [id uuid language] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:language language}
                    :where [:= :id (:id course)]})
    (let [applied (update/apply course {:type :set-language
                                        :payload update})]
      (add-change! changes-api ds applied :set-language update nil)
      applied)))

(defn -set-description [changes-api ds {:keys [id uuid description] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:description description}
                    :where [:= :id (:id course)]})
    (let [applied (update/apply course {:type :set-description
                                        :payload update})]
      (add-change! changes-api ds applied :set-description update nil)
      applied)))

(defn -set-publishable [changes-api ds {:keys [id uuid publishable] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:publishable publishable}
                    :where [:= :id (:id course)]})
    (let [applied (update/apply course {:type :set-publishable
                                        :payload update})]
      (add-change! changes-api ds applied :set-publishable update nil)
      applied)))

(defn -set-visible [changes-api ds {:keys [id uuid visible] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:visible visible}
                    :where [:= :id (:id course)]})
    (let [applied (update/apply course {:type :set-visible
                                        :payload update})]
      (add-change! changes-api ds applied :set-visible update nil)
      applied)))

(defn -set-review-pending [changes-api ds {:keys [id uuid review-pending] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:review-pending review-pending}
                    :where [:= :id (:id course)]})
    (let [applied (update/apply course {:type :set-review-pending
                                        :payload update})]
      (add-change! changes-api ds applied :set-review-pending update nil)
      applied)))

(defn -set-creator-id [changes-api ds {:keys [id uuid creator-id] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:creator-id creator-id}
                    :where [:= :id (:id course)]})
    (let [applied (update/apply course {:type :set-creator-id
                                        :payload update})]
      (add-change! changes-api ds applied :set-creator-id update nil)
      applied)))

(comment
  (def ds (db/ds :master))

  (-lookup ds {:id 1})
  (-find ds {:id 1})
  (-all ds)
  :end)
