(ns abantu.routes.api.auth
  (:require [abantu.routes.openapi :as api]
            [ring.util.response :as res]
            [abantu.services.auth :as auth]
            [abantu.migrate :as migrate]
            [abantu.services.users :as users]
            [abantu.email.gmail :as gmail]
            [abantu.email.templates :as templates]
            [abantu.config :as conf]))

(defn jag
  {:summary "Register a user as a student"
   :responses (-> (api/success api/User))}
  [{:keys [ds user] :as _request}]
  (res/response (users/user ds (:id user))))

(defn register-student
  {:summary "Registers a user as a student like right now bro"
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

(defn creator-request
  {:summary "request admission to become a course creator"
   :parameters (api/params :body [:map
                                  [:email :string]
                                  [:firstname :string]
                                  [:lastname :string]
                                  [:message :string]])
   :responses (api/success (api/response-schema))}
  [{:keys [ds body] :as _request}]
  (users/register-creator! ds body)
  (gmail/send-email {:to (conf/read-value :email :username)
                     :subject "Abantu - Creator Admission Request"
                     :body (templates/creator-admission-request body)
                     :type :text/html})
  (gmail/send-email {:to (:email body)
                     :subject "Abantu - We received your request!"
                     :body (templates/creator-admission-request-acknowledgement (:firstname body))
                     :type :text/html})
  (res/response {:message "Creator request submitted successfully"}))

(defn set-password
  {:summary "Set a new password using a reset hash"
   :parameters (api/params :body api/SetPasswordParams)
   :responses (-> (api/success (api/response-schema))
                  (api/not-found (api/response-schema)))}
  [{:keys [ds body] :as _request}]
  (prn "here")
  (if (users/set-password! ds body)
    (res/response {:message "Password set successfully"})
    (-> (res/response {:message "Invalid or expired reset link"})
        (res/status 404))))
