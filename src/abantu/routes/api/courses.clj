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