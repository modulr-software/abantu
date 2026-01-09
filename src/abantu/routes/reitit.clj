(ns abantu.routes.reitit
  (:require [reitit.ring :as ring]
            [abantu.db.interface :as db]
            [reitit.coercion.malli]
            [abantu.routes.api.vocab :as vocab]
            [abantu.routes.api.units :as units]
            [abantu.routes.api.courses :as courses]
            [abantu.routes.api.auth :as auth]
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

         ;;auth
         ["/auth/register/student" (-> (post auth/register-student))]
         ["/auth/login" (-> (post auth/login))]
         ["/auth/email/verify" (-> (post auth/verify-email))]
         
         ;; vocab
         ["/vocab" (-> (get vocab/get-all)
                       (post vocab/add))]
         ["/vocab/:id" (-> (get vocab/get-one)
                           (post vocab/update)
                           (delete vocab/delete))]
         
         ;;courses
         ["/courses" (-> (get courses/get-all-courses)
                         (post courses/create-course))]
         
         ["/courses/:id" (-> (get courses/get-course)
                             (post courses/update-course)
                             (delete courses/delete-course))]
         
         ["/courses/:id/units" (-> (get units/get-units)
                                   (post units/create-units))]

         ;;units
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
