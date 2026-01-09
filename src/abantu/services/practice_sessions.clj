(ns abantu.services.practice-sessions
  (:require [abantu.db.interface :as db]))

(defn insert-practice-session! [ds practice-session]
  (db/insert! ds {:tname :practice-sessions
                  :data practice-session}))

(comment 

  ())
