(ns abantu.routes.api.auth
  (:require [abantu.routes.openapi :as api]
            [ring.util.response :as res]
            [abantu.services.auth :as auth]
            [abantu.migrate :as migrate]))


(defn register-student
  {:summary "Registers a user as a student"
   :parameters (api/params :body api/RegisterStudentParams)
   :responses (-> (api/success api/RegisterStudentResponse)
                  (api/unauthorized [:map [:message :string]]))}
  [{:keys [ds body] :as _request}]
  (let [{:keys [success error]} (auth/can-register-user? ds body)]
    (if success
      (let [result (auth/register-noob! ds (-> (dissoc body :confirm-password :device-uuid)
                                               (assoc :role "student")))
            {:keys [success error]} (migrate/create-student-db! (get-in result [:user :id]))]
        (if success
          (res/response result)
          (-> (res/response {:message "could not create user database!"
                             :error error})
              (res/status 500))))
      (-> (res/response {:message error})
          (res/status 403)))))

(defn login
  {:summary "log in any user"
   :parameters (api/params :body api/LoginParams)
   :responses (-> (api/success api/LoginResponse)
                  (api/unauthorized (api/response-schema)))}
  [{:keys [ds body] :as _request}]
  (prn "login" body)
  (let [{:keys [success data error]} (auth/login-user ds body)]
    (if success
      (res/response data)
      (-> (res/response {:message error})
          (res/status 403)))))

(defn verify-email
  {:summary "verify user email"
   :parameters (api/params :body api/EmailVerificationParams)
   :responses (-> (api/success (api/response-schema))
                  (api/not-found (api/response-schema)))}
  [{:keys [ds body] :as _request}]
  (let [{:keys [email-hash]} body]
    (if (auth/verify-email ds email-hash)
      (res/response {:message "successfully verified email"})
      (-> (res/response {:message "the email hash provided does not match an existing user"})
          (res/status 404)))))