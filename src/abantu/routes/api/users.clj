(ns abantu.routes.api.users
  (:require [abantu.routes.openapi :as api]
            [abantu.services.users :as users]
            [abantu.email.gmail :as gmail]
            [abantu.email.templates :as templates]
            [ring.util.response :as res]))

(defn get-all-users
  {:summary "Get a list of all users. Requires a role above student."
   :responses (api/success api/GetUsersResponse)}
  [{:keys [ds] :as _request}]
  (res/response (users/get-all-users ds)))

(defn add-user
  {:summary "Add a new user with a specified role. Admin only."
   :parameters (api/params :body api/AddUserParams)
   :responses (-> (api/success [:map [:message :string]])
                  (api/response 400 (api/error)))}
  [{:keys [ds body] :as _request}]
  (try
    (users/create-user! ds body)
    (res/response {:message "Successfully created user"})
    (catch Exception e
      (prn e)
      (-> (res/response {:message "Unable to create user"})
          (res/status 400)))))

(defn archive-user
  {:summary "Archive a user by id. Admin only."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success (api/response-schema))
                  (api/response 400 (api/error)))}
  [{:keys [ds path-params] :as _request}]
  (try
    (users/archive-user! ds (:id path-params))
    (res/response {:message "Successfully archived user"})
    (catch Exception e
      (prn e)
      (-> (res/response {:message "Unable to archive user"})
          (res/status 400)))))

(defn unarchive-user
  {:summary "Unarchive a user by id. Admin only."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success (api/response-schema))
                  (api/response 400 (api/error)))}
  [{:keys [ds path-params] :as _request}]
  (try
    (users/unarchive-user! ds (:id path-params))
    (res/response {:message "Successfully unarchived user"})
    (catch Exception e
      (prn e)
      (-> (res/response {:message "Unable to unarchive user"})
          (res/status 400)))))

(defn approve-user
  {:summary "Approve a creator user by id. Admin only."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success (api/response-schema))
                  (api/response 400 (api/error)))}
  [{:keys [ds path-params] :as _request}]
  (try
    (let [id (:id path-params)
          user (users/get-user ds id)
          hash (users/set-password-reset-hash! ds id)
          set-password-url (str "https://abantu-portal.modulrza.app/register/set-password/" hash)]
      (users/approve-user! ds id)
      (gmail/send-email {:to (:email user)
                         :subject "Abantu - Your Creator Account Has Been Approved!"
                         :body (templates/creator-approved {:firstname (:firstname user)
                                                           :set-password-url set-password-url})
                         :type :text/html})
      (res/response {:message "Successfully approved user"}))
    (catch Exception e
      (prn e)
      (-> (res/response {:message "Unable to approve user"})
          (res/status 400)))))
