(ns abantu.services.courses.core
  (:require [abantu.db.interface :as db]
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

;; TODO: move this to a shared file
(defn- process-bools [course]
  (util/parse-bool-keys course [:publishable :visible :review-pending]))

(defn -lookup [ds {:keys [id uuid]}]
  (->> (db/find ds (cond-> {:tname :courses
                            :ret :1}
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

(defn -create [ds {:keys [uuid units] :as update}]
  (let [{:keys [id]} (db/insert! ds {:tname :courses
                                     :values (-> (dissoc update :units)
                                                 (assoc :uuid (or uuid (util/uuid))))
                                     :ret :1})]
    (run! #(unit/create (unit/use-mutation ds) %) units)
    (update/apply (-lookup ds {:id id}) {:type :create
                                         :payload update})))

(defn -delete [ds {:keys [id uuid] :as update}]
  (let [{:keys [id]} (-lookup ds {:id id :uuid uuid})
        unmut (unit/use-mutation ds)
        unit-ids (db/find ds {:tname :units
                              :where [:= :course-id id]})]
    (run! #(unit/delete unmut %) unit-ids)
    (db/delete! ds {:tname :user-courses
                    :where [:= :course-id id]})
    (db/delete! ds {:tname :course-editors
                    :where [:= :course-id id]})
    (db/delete! ds {:tname :courses
                    :where [:= :id id]})
    (update/apply nil {:type :delete
                       :payload update})))

(defn -set-name [ds {:keys [id uuid name] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:name name}
                    :where [:= :id (:id course)]})
    (update/apply course {:type :set-name
                          :payload update})))

(defn -set-language [ds {:keys [id uuid language] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:language language}
                    :where [:= :id (:id course)]})
    (update/apply course {:type :set-language
                          :payload update})))

(defn -set-description [ds {:keys [id uuid description] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:description description}
                    :where [:= :id (:id course)]})
    (update/apply course {:type :set-description
                          :payload update})))

(defn -set-publishable [ds {:keys [id uuid publishable] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:publishable publishable}
                    :where [:= :id (:id course)]})
    (update/apply course {:type :set-publishable
                          :payload update})))

(defn -set-visible [ds {:keys [id uuid visible] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:visible visible}
                    :where [:= :id (:id course)]})
    (update/apply course {:type :set-visible
                          :payload update})))

(defn -set-review-pending [ds {:keys [id uuid review-pending] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:review-pending review-pending}
                    :where [:= :id (:id course)]})
    (update/apply course {:type :set-review-pending
                          :payload update})))

(defn -set-creator-id [ds {:keys [id uuid creator-id] :as update}]
  (when-let [course (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :courses
                    :values {:creator-id creator-id}
                    :where [:= :id (:id course)]})
    (update/apply course {:type :set-creator-id
                          :payload update})))

(comment
  (def ds (db/ds :master))

  (-lookup ds {:id 1})
  (-find ds {:id 1})
  (-all ds)
  :end)
