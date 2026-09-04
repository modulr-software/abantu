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

(defn -lookup [ds {:keys [id]}]
  (->> (db/find ds (cond-> {:tname :courses
                            :ret :1}
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

(defn -find [ds {:keys [id creator-id]}]
  (->> (db/find ds (cond-> {:tname :courses
                            :ret :*}
                     (some? id) (h/where [:= :id id])
                     (some? creator-id) (h/where [:= :creator-id creator-id])))
       (mapv
        (comp process-bools
              (partial attach-units ds)
              (partial attach-creator ds)))))

(defn -create [ds {:keys [units] :as update}]
  (let [{:keys [id]} (db/insert! ds {:tname :courses
                                     :values (dissoc update :units)
                                     :ret :1})]
    (run! #(unit/create (unit/use-mutation ds) %) units)
    (update/apply (-lookup ds {:id id}) {:type :create
                                         :payload update})))

(defn -delete [ds {:keys [id] :as update}]
  (let [unmut (unit/use-mutation ds)
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

(defn -set-name [ds {:keys [id name] :as update}]
  (when-let [course (-lookup ds {:id id})]
    (db/update! ds {:tname :courses
                    :values {:name name}})
    (update/apply course {:type :set-name
                          :payload update})))

(defn -set-language [ds {:keys [id language] :as update}]
  (when-let [course (-lookup ds {:id id})]
    (db/update! ds {:tname :courses
                    :values {:language language}})
    (update/apply course {:type :set-language
                          :payload update})))

(defn -set-description [ds {:keys [id description] :as update}]
  (when-let [course (-lookup ds {:id id})]
    (db/update! ds {:tname :courses
                    :values {:description description}})
    (update/apply course {:type :set-description
                          :payload update})))

(defn -set-publishable [ds {:keys [id publishable] :as update}]
  (when-let [course (-lookup ds {:id id})]
    (db/update! ds {:tname :courses
                    :values {:publishable publishable}})
    (update/apply course {:type :set-publishable
                          :payload update})))

(defn -set-visible [ds {:keys [id visible] :as update}]
  (when-let [course (-lookup ds {:id id})]
    (db/update! ds {:tname :courses
                    :values {:visible visible}})
    (update/apply course {:type :set-visible
                          :payload update})))

(defn -set-review-pending [ds {:keys [id review-pending] :as update}]
  (when-let [course (-lookup ds {:id id})]
    (db/update! ds {:tname :courses
                    :values {:review-pending review-pending}})
    (update/apply course {:type :set-review-pending
                          :payload update})))

(defn -set-creator-id [ds {:keys [id creator-id] :as update}]
  (when-let [course (-lookup ds {:id id})]
    (db/update! ds {:tname :courses
                    :values {:creator-id creator-id}})
    (update/apply course {:type :set-creator-id
                          :payload update})))

(comment
  (def ds (db/ds :master))

  (-lookup ds {:id 1})
  (-find ds {:id 1})
  (-all ds)
  :end)
