(ns abantu.routes.api.users
  (:require [abantu.routes.openapi :as api]
            [abantu.services.users :as users]
            [ring.util.response :as res]))

(defn get-all-users
  {:summary "Get a list of all users. Requires a role above student."
   :responses (api/success api/GetUsersResponse)}
  [{:keys [ds] :as _request}]
  (res/response (users/get-all-users ds)))
