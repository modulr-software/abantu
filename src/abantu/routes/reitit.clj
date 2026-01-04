(ns abantu.routes.reitit
  (:require [reitit.ring :as ring]
            [abantu.middleware.interface :as mw]
            [abantu.db.interface :as db]
            [reitit.coercion.malli]
            [abantu.routes.api.vocab :as vocab]
            [abantu.routes.api.units :as units]
            [abantu.routes.util :refer [get post delete] :as rutil]))


(defn create-app
  ([] (create-app {:ds (db/ds :master)}))
  ([{:keys [ds]}]
   (let [ds (or ds (db/ds :master))]
     (ring/ring-handler
      (ring/router
       [(rutil/swagger-route)
        (rutil/openapi-route)

        ["/api" 
         ;; vocab
         ["/vocab" (-> (get vocab/get-all)
                       (post vocab/add))]
         ["/vocab/:id" (-> (get vocab/get-one)
                           (post vocab/update)
                           (delete vocab/delete))]

         ;;units
         ["/units" (-> (get units/get-all)
                       (post units/create-units))]

         ["/units/:id" (-> (get units/get-by-id)
                           (delete units/delete-unit)
                           (post units/update-unit))]


         ;;exercises
         ["/units/:id/exercises" (-> (get units/get-exercises-for-unit)
                                     (post units/add-exercises-to-unit))]

         ["/exercises/:id" (-> (get units/get-exercise)
                               (post units/update-exercise)
                               (delete units/delete-exercise))]]]
       (rutil/data-map ds))
      (ring/routes
       (rutil/swagger-ui-handler)
       (ring/create-default-handler))))))

(comment
  ())
