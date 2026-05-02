(ns abantu.routes.reitit
  (:require [reitit.ring :as ring]
            [abantu.db.interface :as db]
            [reitit.coercion.malli]
            [abantu.routes.api.vocab :as vocab]
            [abantu.routes.api.units :as units]
            [abantu.routes.api.courses :as courses]
            [abantu.routes.api.auth :as auth]
            [abantu.routes.api.student :as student]
            [abantu.middleware.auth.core :as authmw]
            [abantu.routes.util :refer [get post delete tag mw] :as rutil]))

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
         ["/auth/register/student" (-> (post auth/register-student)
                                       (tag :auth))]

         ["/auth/jag" (-> (get auth/jag)
                          (mw authmw/wrap-auth)
                          (tag :auth))]

         ["/auth/login" (-> (post auth/login)
                            (tag :auth))]

         ["/auth/email/verify" (-> (post auth/verify-email)
                                   (tag :auth))]

         ["/student/session/start/:id" (-> (post student/start-session!)
                                           (mw authmw/wrap-auth)
                                           (tag :student))]
         ["/student/session/end/:id" (-> (post student/end-session!)
                                         (mw authmw/wrap-auth)
                                         (tag :student))]

         ["/student/courses" (-> (get student/get-courses)
                                 (mw authmw/wrap-auth)
                                 (tag :student))]

         ["/courses/:id/instructions" (-> (get courses/used-instructions))]

         ["/student/subscribable" (-> (get student/subscribable-courses)
                                      (mw authmw/wrap-auth)
                                      (tag :student))]

         ["/student/courses/:id" (-> (get student/get-course)
                                     (post student/assign-course!)
                                     (delete student/remove-course!)
                                     (mw authmw/wrap-auth)
                                     (tag :student))]

;; vocab
         ["/vocab" (-> (get vocab/get-all)
                       (post vocab/add)
                       (tag :vocab))]

         ["/vocab/:id" (-> (get vocab/get-one)
                           (post vocab/update)
                           (delete vocab/delete)
                           (tag :vocab))]

         ;;courses
         ["/courses" (-> (get courses/get-all-courses)
                         (post courses/create-course)
                         (mw authmw/wrap-auth)
                         (tag :courses))]

         ["/courses/:id" (-> (get courses/get-course)
                             (post courses/update-course)
                             (delete courses/delete-course)
                             (mw authmw/wrap-auth)
                             (tag :courses))]

         ["/courses/:id/units" (-> (get units/get-units)
                                   (post units/create-units)
                                   (mw authmw/wrap-auth)
                                   (tag :units :courses))]

         ;;units
         ["/units/:id" (-> (get units/get-by-id)
                           (delete units/delete-unit)
                           (post units/update-unit)
                           (mw authmw/wrap-auth)
                           (tag :units))]

;;exercises
         ["/units/:id/exercises" (-> (get units/get-exercises-for-unit)
                                     (post units/add-exercises-to-unit)
                                     (tag :exercises))]
         
         ["/units/:id/exercises/move" (-> (post units/move-exercises))]

         ["/exercises/:id" (-> (get units/get-exercise)
                               (post units/update-exercise)
                               (delete units/delete-exercise)
                               (tag :exercises))]]]

       (rutil/data-map ds))
      (ring/routes
       (rutil/swagger-ui-handler)
       (ring/create-default-handler))))))

(comment
  ())
