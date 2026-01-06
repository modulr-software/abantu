(ns abantu.routes.api.courses
  (:require [abantu.routes.openapi :as api]
            [abantu.services.courses :as courses]
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

(defn create-course
  {:summary "Create a new course"
   :parameters (api/params :body api/CreateCourseParam)
   :responses (api/success api/GetCourseResponse)
   :tech/debt "replace creator-id with a real user-id, probably gotten from authz middleware"}
  [{:keys [ds body] :as _request}]
  (let [course (merge body {:status "in-progress"
                            :creator-id nil})]
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
