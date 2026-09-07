(ns abantu.services.changes.core
  (:require [abantu.db.interface :as db]
            [abantu.util :as util]
            [clojure.data.json :as json]))

(defn- attach-courses [ds {:keys [id] :as version}]
  (assoc version :course-changes (db/find ds {:tname :course-changes
                                              :where [:= :version-id id]
                                              :ret :*})))

(defn- attach-units [ds {:keys [id] :as version}]
  (assoc version :unit-changes (db/find ds {:tname :unit-changes
                                            :where [:= :version-id id]
                                            :ret :*})))

(defn- attach-exercises [ds {:keys [id] :as version}]
  (assoc version :exercise-changes (db/find ds {:tname :exercise-changes
                                                :where [:= :version-id id]
                                                :ret :*})))

(defn -lookup [ds {:keys [_id _timestamp with-changes?] :as opts}]
  (cond->> (-> (db/find-one ds {:tname :versions
                                :where (util/eq-clauses (dissoc opts :with-changes?))})
               (util/parse-bool-keys [:applied]))
    with-changes? (attach-courses ds)
    with-changes? (attach-units ds)
    with-changes? (attach-exercises ds)))

(defn -find [ds {:keys [_course-id _label with-changes?] :as opts}]
  (cond->> (db/find ds {:tname :versions
                        :where (util/eq-clauses (dissoc opts :with-changes?))
                        :ret :*})
    true (mapv #(util/parse-bool-keys % [:applied]))
    with-changes? (mapv (comp (partial attach-courses ds)
                              (partial attach-units ds)
                              (partial attach-exercises ds)))))

(defn -all [ds {:keys [with-changes?] :as _opts}]
  (cond->> (db/find ds {:tname :versions
                        :ret :*})
    true (mapv #(util/parse-bool-keys % [:applied]))
    with-changes? (mapv (comp (partial attach-courses ds)
                              (partial attach-units ds)
                              (partial attach-exercises ds)))))

(defn -exercises [ds {:keys [_version-id _timestamp] :as opts}]
  (db/find ds {:tname :exercise-changes
               :where (util/eq-clauses opts)
               :ret :*}))

(defn -units [ds {:keys [_version-id _timestamp] :as opts}]
  (db/find ds {:tname :unit-changes
               :where (util/eq-clauses opts)
               :ret :*}))

(defn -courses [ds {:keys [_version-id _timestamp] :as opts}]
  (db/find ds {:tname :course-changes
               :where (util/eq-clauses opts)
               :ret :*}))

(defn -add-version! [ds {:keys [label course-id] :as _payload}]
  (let [{:keys [id]} (db/insert! ds {:tname :versions
                                     :data {:label label
                                            :course-id course-id
                                            :timestamp (util/get-utc-timestamp-string)}
                                     :ret :1})]
    (-lookup ds {:id id})))

(defn -set-version-label! [ds {:keys [id label]}]
  (db/update! ds {:tname :versions
                  :data {:label label}
                  :where [:= :id id]}))

(defn -add-course-update! [ds {:keys [version-id change-type update]}]
  (let [change-type (str change-type)
        id (:id update)
        json (json/write-str (dissoc update :id))]
    (db/insert! ds {:tname :course-changes
                    :data {:course-id id
                           :change-type change-type
                           :change-data json
                           :timestamp (util/get-utc-timestamp-string)
                           :version-id version-id}
                    :ret :1})))

(defn -add-unit-update! [ds {:keys [version-id change-type update]}]
  (let [change-type (str change-type)
        {:keys [id course-id]} update
        json (json/write-str (dissoc update :id :course-id))]
    (db/insert! ds {:tname :unit-changes
                    :data {:unit-id id
                           :course-id course-id
                           :change-type change-type
                           :change-data json
                           :timestamp (util/get-utc-timestamp-string)
                           :version-id version-id}
                    :ret :1})))

(defn -add-exercise-update! [ds {:keys [version-id change-type update]}]
  (let [change-type (str change-type)
        {:keys [id unit-id course-id]} update
        json (json/write-str (dissoc update :id :course-id))]
    (db/insert! ds {:tname :exercise-changes
                    :data {:exercise-id id
                           :unit-id unit-id
                           :course-id course-id
                           :change-type change-type
                           :change-data json
                           :timestamp (util/get-utc-timestamp-string)
                           :version-id version-id}
                    :ret :1})))

(defn migrate-up! [ds {:keys [course-id version-id]}])
