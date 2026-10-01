(ns abantu.services.comments.core
  (:require [abantu.db.honey :as hon]
            [honey.sql :as h]))

(defn -lookup [ds {:keys [id]}]
  (hon/find-one ds {:tname :comments
                    :where [:= :id id]}))

(defn -find [ds {:keys [_course-id _unit-id _exercise-id] :as opts}]
  (hon/find ds {:tname :comments
                :where (h/map= opts)}))

(defn -all [ds]
  (hon/find ds {:tname :comments}))
