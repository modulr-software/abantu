(ns abantu.routes.api.courses
  (:require [abantu.routes.openapi :as api]
            [abantu.services.courses :as courses]
            [abantu.services.users :as users]
            [ring.util.response :as res]))

(defn get-all-courses
  {:summary "Get all courses"
   :responses (api/success api/GetCoursesResponse)}
  [{:keys [ds] :as _request}]
  (res/response
   (courses/get-all ds)))

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
  {:summary "Create a new course"
   :parameters (api/params :body api/CreateCourseParam)
   :responses (api/success api/GetCourseResponse)}
  [{:keys [ds body user] :as _request}]
  (let [course (merge body {:status "in-progress"
                            :creator-id (:id user)})]
    (res/response (courses/save-course! ds course))))

(defn update-course
  {:summary "Update course details for the given course by id"
   :parameters (api/params :path api/IdPathParam :body api/UpdateCourseParam)
   :responses (-> (api/success api/GetCourseResponse)
                  (api/not-found))}
  [{:keys [ds body path-params] :as _request}]
  (let [{:keys [id]} path-params
        exists? (courses/update-course! ds (assoc body :id id))
        course (courses/get-course ds id)]
    (if exists?
      (res/response course)
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
