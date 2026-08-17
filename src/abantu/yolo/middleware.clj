(ns abantu.yolo.middleware
  (:require [abantu.services.users :as users]
            [abantu.db.interface :as db]))


(defn wrap-auth-if-exists [handler]
  (fn [req]
    (let [payload (abantu.middleware.auth.core/validate-request req)]
      (assoc req :user (when payload (users/get-user (db/ds :master)
                                                      (:id payload)))))))