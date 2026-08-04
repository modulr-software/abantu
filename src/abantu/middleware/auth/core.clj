(ns abantu.middleware.auth.core
  (:require [abantu.middleware.auth.util :as util]
            [abantu.db.util :as db.util]
            [ring.util.response :as res]
            [abantu.services.users :as users]
            [abantu.services.courses :as courses]))

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
  (fn [{:keys [ds] :as request}]
    (if-let [user (validate-request request)]
      (let [db-user (users/get-user ds (:id user))]
        (cond
          (:archived db-user)
          (-> (res/response {:message "Forbidden: this user account has been archived."})
              (res/status 403))

          (and (= "creator" (:role db-user)) (not (:approved db-user)))
          (-> (res/response {:message "Forbidden: this user account has not been approved."})
              (res/status 403))

          :else
          (-> request
              (assoc :user user)
              (handler))))
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
      (let [db-user (users/get-user ds (:id user))
            role (:role db-user)]
        (cond
          (:archived db-user)
          (-> (res/response {:message "Forbidden: this user account has been archived."})
              (res/status 403))

          (and (= "creator" role) (not (:approved db-user)))
          (-> (res/response {:message "Forbidden: this user account has not been approved."})
              (res/status 403))

          (and (some? role) (not= "student" role))
          (-> request
              (assoc :user user)
              (handler))

          :else
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

(defn wrap-admin
  "Ring middleware that only lets through authenticated users whose role is 'admin'.
   Rejects all others with 403."
  [handler]
  (fn [{:keys [ds] :as request}]
    (if-let [user (validate-request request)]
      (let [db-user (users/get-user ds (:id user))
            role (:role db-user)]
        (cond
          (:archived db-user)
          (-> (res/response {:message "Forbidden: this user account has been archived."})
              (res/status 403))

          (= "admin" role)
          (-> request
              (assoc :user user)
              (handler))

          :else
          (-> (res/response {:message "Forbidden: admin access required."})
              (res/status 403))))
      (-> (res/response {:message "Unauthorized"})
          (res/status 401)))))

(defn wrap-owner-or-admin
  "Ring middleware that only lets through authenticated users who are admins or the
   creator of the course identified by the :id path param. Assocs :user and :course
   onto the request. Responds 401 when unauthenticated, 404 when the course does not
   exist, and 403 when the user is neither an admin nor the course creator."
  [handler]
  (fn [{:keys [ds path-params] :as request}]
    (if-let [user (validate-request request)]
      (let [db-user (users/get-user ds (:id user))
            role (:role db-user)
            course (courses/get-course ds (:id path-params))]
        (cond
          (:archived db-user)
          (-> (res/response {:message "Forbidden: this user account has been archived."})
              (res/status 403))

          (and (= "creator" role) (not (:approved db-user)))
          (-> (res/response {:message "Forbidden: this user account has not been approved."})
              (res/status 403))

          (nil? course)
          (-> (res/response {:message (str "The course with the id '" (:id path-params) "' does not exist.")})
              (res/status 404))

          (or (= "admin" role)
              (= (:id user) (get-in course [:creator :id])))
          (-> request
              (assoc :user user :course course)
              (handler))

          :else
          (-> (res/response {:message "Forbidden: you are not the creator of this course."})
              (res/status 403))))
      (-> (res/response {:message "Unauthorized"})
          (res/status 401)))))

(defn wrap-owner-or-editor-or-admin
  "Like wrap-owner-or-admin, but also lets through editors of the course
  (creators with edit rights via the course-editors table). Assocs :user and
  :course onto the request."
  [handler]
  (fn [{:keys [ds path-params] :as request}]
    (if-let [user (validate-request request)]
      (let [db-user (users/get-user ds (:id user))
            role (:role db-user)
            course (courses/get-course ds (:id path-params))]
        (cond
          (:archived db-user)
          (-> (res/response {:message "Forbidden: this user account has been archived."})
              (res/status 403))

          (and (= "creator" role) (not (:approved db-user)))
          (-> (res/response {:message "Forbidden: this user account has not been approved."})
              (res/status 403))

          (nil? course)
          (-> (res/response {:message (str "The course with the id '" (:id path-params) "' does not exist.")})
              (res/status 404))

          (or (= "admin" role)
              (= (:id user) (get-in course [:creator :id]))
              (courses/editor? ds (:id user) (:id course)))
          (-> request
              (assoc :user user :course course)
              (handler))

          :else
          (-> (res/response {:message "Forbidden: you are not the creator or an editor of this course."})
              (res/status 403))))
      (-> (res/response {:message "Unauthorized"})
          (res/status 401)))))

(defn wrap-publishable
  "Ring middleware that only lets through requests for a course (identified by the :id
   path param) that is publishable. Assocs :course onto the request. Must be composed
   after an auth middleware (e.g. wrap-admin or wrap-owner-or-admin). Responds 404 when
   the course does not exist and 400 when the course is not publishable."
  [handler]
  (fn [{:keys [ds path-params] :as request}]
    (let [course (courses/get-course ds (:id path-params))]
      (cond
        (nil? course)
        (-> (res/response {:message (str "The course with the id '" (:id path-params) "' does not exist.")})
            (res/status 404))

        (not (:publishable course))
        (-> (res/response {:message "Bad request: this course is not publishable."})
            (res/status 400))

        :else
        (-> request
            (assoc :course course)
            (handler))))))

(comment

  ())
