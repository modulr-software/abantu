(ns abantu.routes.reitit
  (:require [reitit.ring :as ring]
            [abantu.db.interface :as db]
            [reitit.coercion.malli]
            [abantu.routes.api.vocab :as vocab]
            [abantu.routes.api.units :as units]
            [abantu.routes.api.courses :as courses]
            [abantu.routes.api.users :as users]
            [abantu.routes.api.auth :as auth]
            [abantu.routes.api.student :as student]
            [abantu.routes.api.audio :as audio]
            [abantu.routes.api.spamtest :as spamtest]
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

         ["/spamtest" (-> (get spamtest/get-spamtest)
                          (tag :spamtest))]

         ["/media/audio" (-> (post audio/upload-audio)
                             (get audio/get-audio)
                             (tag :audio))]

         ["/media/audio/blob" (-> (get audio/get-blob)
                                  (tag :audio))]

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

          ["/auth/creator/request" (-> (post auth/creator-request)
                                       (tag :auth))]

          ["/auth/password/set" (-> (post auth/set-password)
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

          ;;users
          ["/users" (-> (get users/get-all-users)
                        (mw authmw/wrap-above-student)
                        (tag :users))]

          ["/users/add" (-> (post users/add-user)
                            (mw authmw/wrap-admin)
                            (tag :users :admin))]

          ["/users/archive/:id" (-> (delete users/archive-user)
                            (mw authmw/wrap-admin)
                            (tag :users :admin))]

          ["/users/unarchive/:id" (-> (post users/unarchive-user)
                                      (mw authmw/wrap-admin)
                                      (tag :users :admin))]

          ["/users/approve/:id" (-> (post users/approve-user)
                                    (mw authmw/wrap-admin)
                                    (tag :users :admin))]

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
                             (delete courses/delete-course)
                             (mw authmw/wrap-auth)
                             (tag :courses))]

         ["/courses/:id/update" (-> (post courses/update-course)
                                    (mw authmw/wrap-owner-or-admin)
                                    (tag :courses))]

         ["/courses/:id/publish/request" (-> (post courses/request-publish)
                                             (mw authmw/wrap-owner-or-admin)
                                             (mw authmw/wrap-publishable)
                                             (tag :courses))]

         ["/courses/:id/publish/approve" (-> (post courses/approve-publish)
                                             (mw authmw/wrap-admin)
                                             (mw authmw/wrap-publishable)
                                             (tag :courses :admin))]

         ["/courses/:id/publish/hide" (-> (post courses/hide-publish)
                                          (mw authmw/wrap-admin)
                                          (tag :courses :admin))]

         ["/courses/:id/units" (-> (get units/get-units)
                                   (post units/create-units)
                                   (mw authmw/wrap-auth)
                                   (tag :units :courses))]

         ["/courses/:id/units/order/change" (-> (post courses/change-units-order)
                                                (mw authmw/wrap-auth)
                                                (tag :courses))]

         ["/courses/:id/users/:user-id" (-> (post courses/assign-user-to-course!)
                                            (delete courses/remove-user-from-course!)
                                            (mw authmw/wrap-auth)
                                            (tag :courses))]

         ["/courses/:id/students" (-> (get courses/get-course-students)
                                      (mw authmw/wrap-above-student)
                                      (tag :courses))]

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

         ["/units/:id/exercises/order/change" (-> (post units/change-exercises-order)
                                                  (mw authmw/wrap-auth)
                                                  (tag :exercises))]

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
