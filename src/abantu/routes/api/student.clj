(ns abantu.routes.api.student
  (:require [ring.util.response :as res]
            [abantu.services.courses :as courses]
            [abantu.routes.openapi :as api]
            [clj-oauth2.client :as oauth2]))


(defn get-courses
  {:summary "Get all courses a student has started for a given student id"
   :responses (-> (api/success api/GetCoursesResponse))}
  [{:keys [ds user] :as _request}]
    (res/response (courses/courses-by-user ds (:id user))))

(defn get-course
  {:summary "Get course progress info for a given course with units"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success api/GetCourseResponse)}
  [{:keys [ds user path-params] :as _request}]
  (let [{:keys [id]} path-params]
    (res/response
     (courses/course-by-user ds (:id user) id))))

