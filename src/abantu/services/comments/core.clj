(ns abantu.services.comments.core
  (:require [abantu.db.honey :as hon]
            [honey.sql :as h]
            [abantu.util :as util]))

(defn process-bools [exercise]
  (util/parse-bool-keys exercise [:resolved]))

(defn -lookup [ds {:keys [id]}]
  (->> (hon/find-one ds {:tname :comments
                         :where [:= :id id]})
       (process-bools)))

(defn -find [ds {:keys [_course-id _unit-id _exercise-id _resolved] :as opts}]
  (->> (hon/find ds {:tname :comments
                     :where (h/map= opts)})
       (mapv process-bools)))

(defn -all [ds]
  (->> (hon/find ds {:tname :comments})
       (mapv process-bools)))
