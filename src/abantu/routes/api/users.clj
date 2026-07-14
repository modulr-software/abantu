(ns abantu.routes.api.users
  (:require [abantu.routes.openapi :as api]
            [abantu.services.users :as users]
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
