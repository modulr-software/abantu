(ns abantu.migrations.002-comments
  (:require [abantu.db.master]
            [abantu.db.tables :as tables]))

(defn run-up! [context]
  (let [ds-master (:db-master context)]
    (tables/create-tables!
     ds-master
     :abantu.db.master
     [:comments])))

(defn run-down! [context]
  (let [ds-master (:db-master context)]
    (tables/drop-tables!
     ds-master
     [:comments])))
