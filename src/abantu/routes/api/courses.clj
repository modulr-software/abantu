(ns abantu.routes.api.courses
  (:require [abantu.routes.openapi :as api]
            [abantu.services.courses :as courses]
            [abantu.services.users :as users]
            [ring.util.response :as res]))

(defn get-all-courses
  {:summary "Get all courses. Admins receive every course; users above student receive only
             the courses they created; students are forbidden."
   :responses (-> (api/success api/GetCoursesResponse)
                  (api/response 403 (api/error)))}
  [{:keys [ds user] :as _request}]
  (let [role (:role (users/get-user ds (:id user)))]
    (cond
      (= "admin" role)
      (res/response (courses/get-all ds))

      (and (some? role) (not= "student" role))
      (res/response (courses/courses-by-creator ds (:id user)))

      :else
      (-> (res/response {:message "Forbidden: students are not permitted to view courses."})
          (res/status 403)))))

(defn get-course
  {:summary "Get a specific course with all units by id"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success api/GetCourseResponse)}
  [{:keys [ds path-params] :as _request}]
  (res/response (courses/get-course ds (:id path-params))))

(defn get-course-students
  {:summary "Get all students subscribed to a given course. Requires a role above student."
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success api/GetCourseStudentsResponse)}
  [{:keys [ds path-params] :as _request}]
  (res/response
   (courses/students-by-course ds (parse-long (str (:id path-params))))))

(defn create-course
  {:summary "Create a new course. The creator must choose whether the course is
             publishable; it always starts invisible with no review pending."
   :parameters (api/params :body api/CreateCourseParam)
   :responses (api/success api/GetCourseResponse)}
  [{:keys [ds body user] :as _request}]
  (let [course (-> body
                   (update :publishable #(if % 1 0))
                   (merge {:creator-id (:id user)
                           :visible 0
                           :review-pending 0}))]
    (res/response (courses/save-course! ds course))))

(defn update-course
  {:summary "Update course details for the given course by id. Owner or admin only
             (enforced by middleware). Flipping :publishable resets the visibility
             lifecycle (visible=0, review-pending=0)."
   :parameters (api/params :path api/IdPathParam :body api/UpdateCourseParam)
   :responses (-> (api/success api/GetCourseResponse)
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds body path-params course] :as _request}]
  (let [{:keys [id]} path-params
        flipped? (and (contains? body :publishable)
                      (not= (true? (:publishable body)) (:publishable course)))
        updates (cond-> (dissoc body :creator-id)
                  (contains? body :publishable) (update :publishable #(if % 1 0))
                  flipped? (merge {:visible 0 :review-pending 0}))]
    (when (seq updates)
      (courses/update-course! ds (assoc updates :id id)))
    (res/response (courses/get-course ds id))))

(defn request-publish
  {:summary "Request that a publishable course be made visible to students. Sets
             review-pending; an admin approves via /courses/:id/publish/approve.
             Owner or admin only; the course must be publishable (both enforced
             by middleware)."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/GetCourseResponse)
                  (api/not-found)
                  (api/bad-request)
                  (api/response 403 (api/error)))}
  [{:keys [ds course] :as _request}]
  (courses/request-publish! ds (:id course))
  (res/response (courses/get-course ds (:id course))))

(defn approve-publish
  {:summary "Make a publishable course visible to students and clear review-pending.
             Admin only; the course must be publishable (both enforced by middleware)."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/GetCourseResponse)
                  (api/not-found)
                  (api/bad-request))}
  [{:keys [ds course] :as _request}]
  (courses/approve-publish! ds (:id course))
  (res/response (courses/get-course ds (:id course))))

(defn hide-publish
  {:summary "Make a course invisible to students and clear review-pending. Admin only."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/GetCourseResponse)
                  (api/not-found))}
  [{:keys [ds path-params] :as _request}]
  (let [{:keys [id]} path-params]
    (if-let [course (courses/get-course ds id)]
      (do (courses/hide-publish! ds (:id course))
          (res/response (courses/get-course ds id)))
      (-> (res/response {:message (str "The course with the id '" id "' does not exist.")})
          (res/status 404)))))

(defn delete-course
  {:summary "Delete the course and all associated units with the given course id"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success (api/response-schema))}
  [{:keys [ds path-params] :as _request}]
  (courses/delete-course! ds (:id path-params))
  (res/response {:message "Successfully deleted course"}))

(defn used-instructions
  {:summary "Get all previously entered instructions for exercises in a given course."
   :parameters (api/params :path api/IdPathParam)
   :responses  (api/success [:vector :string])}
  [{:keys [ds path-params] :as _request}]
  (res/response
   (courses/used-instructions ds (:id path-params))))

(defn assign-user-to-course!
  {:summary "Assign a given user to a given course by user-id and course-id.
             The requester must be above a student and the creator of the course."
   :parameters (api/params :path api/CourseUserPathParams)
   :responses (-> (api/success [:map [:message :string]])
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds path-params user] :as _request}]
  (let [course-id (parse-long (str (:id path-params)))
        assignee-id (parse-long (str (:user-id path-params)))
        role (:role (users/get-user ds (:id user)))
        course (courses/get-course ds course-id)]
    (cond
      (nil? course)
      (-> (res/response {:message (str "The course with the id '" course-id "' does not exist.")})
          (res/status 404))

      (not (and role (not= "student" role)))
      (-> (res/response {:message "Unauthorized: your account is not permitted to assign users to courses."})
          (res/status 403))

      (not= (:id user) (get-in course [:creator :id]))
      (-> (res/response {:message "Unauthorized: you are not the creator of this course."})
          (res/status 403))

      :else
      (if (courses/assign-course-to-user! ds assignee-id course-id)
        (res/response {:message (str "Successfully assigned user '" assignee-id "' to course '" course-id "'.")})
        (res/response {:message (str "User '" assignee-id "' is already assigned to this course.")})))))

(defn remove-user-from-course!
  {:summary "Unsubscribe a given user from a given course by user-id and course-id.
             The requester must be above a student and the creator of the course."
   :parameters (api/params :path api/CourseUserPathParams)
   :responses (-> (api/success [:map [:message :string]])
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds path-params user] :as _request}]
  (let [course-id (parse-long (str (:id path-params)))
        assignee-id (parse-long (str (:user-id path-params)))
        role (:role (users/get-user ds (:id user)))
        course (courses/get-course ds course-id)]
    (cond
      (nil? course)
      (-> (res/response {:message (str "The course with the id '" course-id "' does not exist.")})
          (res/status 404))

      (not (and role (not= "student" role)))
      (-> (res/response {:message "Unauthorized: your account is not permitted to unsubscribe users from courses."})
          (res/status 403))

      (not= (:id user) (get-in course [:creator :id]))
      (-> (res/response {:message "Unauthorized: you are not the creator of this course."})
          (res/status 403))

      (not (courses/course-by-user ds assignee-id course-id))
      (res/response {:message (str "User '" assignee-id "' is not subscribed to this course.")})

      :else
      (if (courses/remove-course-from-user! ds assignee-id course-id)
        (res/response {:message (str "Successfully unsubscribed user '" assignee-id "' from course '" course-id "'.")})
        (-> (res/response {:message "Unable to unsubscribe the user from the course."})
            (res/status 500))))))

(defn change-units-order
  {:summary "Set the order of the units in a given course"
   :parameters (api/params :path api/IdPathParam
                           :body [:vector [:map
                                           [:unit-id :int]
                                           [:position :int]]])
   :responses (api/success [:map [:message :string]])}
  [{:keys [ds body] :as _request}]
  (try
    (run! #(courses/change-unit-order ds (:unit-id %) (:position %)) body)
    (res/response {:message "Successfully changed unit order"})
    (catch Exception e
      (prn e)
      (-> (res/response {:message "Unable to change unit order"})
          (res/status 400)))))

(defn list-editors
  {:summary "Get all editors for a given course. Only the course creator or an admin may call this."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success [:vector api/User])
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds path-params] :as _request}]
  (let [course-id (parse-long (str (:id path-params)))]
    (if (courses/get-course ds course-id)
      (res/response (courses/editors-by-course ds course-id))
      (-> (res/response {:message (str "The course with the id '" course-id "' does not exist.")})
          (res/status 404)))))

(defn add-editor
  {:summary "Grant a creator edit rights on a course. Only the course creator or an admin may call this."
   :parameters (api/params :path api/IdPathParam :body api/EditorTargetParams)
   :responses (-> (api/success [:map [:message :string]])
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds path-params body course] :as _request}]
  (let [course-id (parse-long (str (:id path-params)))
        target-id (:user-id body)
        target (users/get-user ds target-id)]
    (cond
      (nil? target)
      (-> (res/response {:message (str "The user with the id '" target-id "' does not exist.")})
          (res/status 404))

      (not= "creator" (:role target))
      (-> (res/response {:message "Only creators can be added as editors."})
          (res/status 403))

      (= (:id target) (get-in course [:creator :id]))
      (-> (res/response {:message "The course creator is already an editor of this course."})
          (res/status 403))

      :else
      (do (courses/add-editor! ds target-id course-id)
          (res/response {:message (str "Successfully added editor '" target-id "' to course '" course-id "'.")})))))

(defn remove-editor
  {:summary "Revoke a creator's edit rights on a course. Only the course creator or an admin may call this."
   :parameters (api/params :path api/IdPathParam :body api/EditorTargetParams)
   :responses (-> (api/success [:map [:message :string]])
                  (api/not-found)
                  (api/response 403 (api/error)))}
  [{:keys [ds path-params body course] :as _request}]
  (let [course-id (parse-long (str (:id path-params)))
        target-id (:user-id body)
        target (users/get-user ds target-id)]
    (cond
      (nil? target)
      (-> (res/response {:message (str "The user with the id '" target-id "' does not exist.")})
          (res/status 404))

      (not= "creator" (:role target))
      (-> (res/response {:message "Only creators can be editors of a course."})
          (res/status 403))

      (not (courses/editor? ds target-id course-id))
      (-> (res/response {:message (str "User '" target-id "' is not an editor of this course.")})
          (res/status 404))

      :else
      (do (courses/remove-editor! ds target-id course-id)
          (res/response {:message (str "Successfully removed editor '" target-id "' from course '" course-id "'.")})))))
