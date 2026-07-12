(ns abantu.middleware.auth.core
  (:require [abantu.middleware.auth.util :as util]
            [abantu.db.util :as db.util]
            [ring.util.response :as res]
            [abantu.services.users :as users]))

(defn create-session [user]
  (let [payload {:id (:id user)
                 :role (:role user)}]
    {:access-token (util/sign-jwt payload)
     :refresh-token (util/sign-jwt payload)}))

(defn validate-request [request]
  (-> request
      (util/auth-token)
      (util/verify-jwt)))

(defn wrap-auth [handler]
  (fn [request]
    (if-let [user (validate-request request)]
      (-> request
          (assoc :user user)
          (handler))
      (->
       (res/response {:message "Unauthorized"})
       (res/status 401)))))

(defn wrap-above-student
  "Ring middleware that only lets through authenticated users whose role is above
   'student' (i.e. creator or admin) and rejects plain student users.

   It is self-contained: it validates the request token itself and assocs :user
   onto the request just like wrap-auth, so it can be attached to any route the
   same way as the other middlewares, e.g. (mw authmw/wrap-above-student).

   Responds 401 when the request is unauthenticated and 403 when the
   authenticated user is a student (or has no elevated role)."
  [handler]
  (fn [{:keys [ds] :as request}]
    (if-let [user (validate-request request)]
      (let [role (:role (users/get-user ds (:id user)))]
        (if (and (some? role) (not= "student" role))
          (-> request
              (assoc :user user)
              (handler))
          (-> (res/response {:message "Forbidden: this action requires a role above student."})
              (res/status 403))))
      (-> (res/response {:message "Unauthorized"})
          (res/status 401)))))

(defn wrap-auth-user-type
  "returns an unauthorized response if the user's type is not the required user type (creator | distributor | admin)"
  [handler & {:keys [required-type]}]
  (fn [request]
    (let [ds (db.util/conn :master)
          user-type (get-in request [:user :type])
          expected-type (->> {:id (get-in request [:user :id])}
                             (users/get-user ds)
                             (:role))]
      (cond
        (not (some? required-type)) (handler request)
        (and (= user-type (name required-type)) (= user-type expected-type)) (handler request)
        :else (->
               (res/response {:message "Unauthorized"})
               (res/status 403))))))

(comment

  ())
