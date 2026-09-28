(ns abantu.services.versions.core
  (:require [abantu.db.interface :as db]
            [abantu.services.changes.core :as changes]
            [abantu.util :as util]))

(defn- attach-courses [ds {:keys [timestamp] :as version}]
  (assoc version :course-changes (changes/-find-course-changes ds {:to timestamp})))

(defn- attach-units [ds {:keys [timestamp] :as version}]
  (assoc version :unit-changes (changes/-find-unit-changes ds {:to timestamp})))

(defn- attach-exercises [ds {:keys [timestamp] :as version}]
  (assoc version :exercise-changes (changes/-find-exercise-changes ds {:to timestamp})))

(defn -lookup [ds {:keys [_id _timestamp with-changes?] :as opts}]
  (when-let [version (db/find-one ds {:tname :versions
                                      :where (util/eq-clauses (dissoc opts :with-changes?))})]
    (cond->> (util/parse-bool-keys version [:applied])
      with-changes? (attach-courses ds)
      with-changes? (attach-units ds)
      with-changes? (attach-exercises ds))))

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

(defn -create! [ds {:keys [label course-id] :as _payload}]
  (let [{:keys [id]} (db/insert! ds {:tname :versions
                                     :data {:label label
                                            :course-id course-id
                                            :timestamp (util/get-utc-timestamp-string)}
                                     :ret :1})]
    (-lookup ds {:id id})))

(defn -set-version-label! [ds {:keys [id label with-changes?]}]
  (db/update! ds {:tname :versions
                  :data {:label label}
                  :where [:= :id id]})
  (-lookup ds {:id id :with-changes? with-changes?}))

