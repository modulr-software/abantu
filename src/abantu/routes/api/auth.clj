(ns abantu.routes.api.auth
  (:require [abantu.routes.openapi :as api]
            [ring.util.response :as res]
            [abantu.services.auth :as auth]))

(defn register-student
  {:summary "Registers a user as a student"
   :parameters (api/params :body api/RegisterStudentParams)
   :responses (-> (api/success api/RegisterStudentResponse)
                (api/unauthorized [:map [:message :string]]))}
  [{:keys [ds body] :as _request}]
  (let [{:keys [success error]} (auth/can-register-user? ds body)
        {:keys [uuid confirm-password] :as user} body]
    (if success
      (res/response (auth/register-noob! ds (dissoc user :confirm-password :uuid)))
      (-> (res/response {:message error})
          (res/status 403)))))

(defn login
  {:summary "log in any user"
   :parameters (api/params :body api/LoginParams)
   :responses (-> (api/success api/LoginResponse)
                  (api/unauthorized [:map [:message :string]]))}
  [{:keys [ds body] :as _request}]
  (let [{:keys [success data error]} (auth/login-user ds body)]
    (if success
      (res/response data)
      (-> (res/response {:message error})
          (res/status 403)))))