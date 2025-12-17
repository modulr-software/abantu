(ns abantu.routes.reitit
  (:require [reitit.ring :as ring]
            [abantu.middleware.interface :as mw]
            [abantu.db.interface :as db]
            [abantu.routes.file :as file]))

(defn create-app
  ([] (create-app {:ds (db/ds :master)}))
  ([{:keys [ds]}]
   (let [ds (or ds (db/ds :master))]
     (ring/ring-handler
      (ring/router
       [["/" {:middleware [[mw/apply-generic :ds ds]]}
         ["" (fn [_request] {:status 200 :body {:message "success"}})]
         ["file" {:post {:handler file/post}}]]])))))

(comment
  ())
