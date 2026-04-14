(ns abantu.routes.api.student
  (:require [ring.util.response :as res]
            [abantu.services.courses :as courses]
            [abantu.routes.openapi :as api]
            [abantu.services.units :as units]))


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

(defn assign-course!
  {:summary "Assign a given course to a given user by course-id"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success [:map [:message :string]])}
  [{:keys [ds user path-params] :as _request}]
  (let [course-id (:id path-params)
        user-id (:id user)]
    (if-let [_user-course (courses/assign-course-to-user! ds user-id course-id)]
      (res/response {:message "Test response"})
      (res/response {:message "nein! this course is already assigned to the user."}))))

(defn remove-course!
  {:summary "Remove a course from a user"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success [:map [:message :string]])}
  [{:keys [ds user path-params] :as _response}]
  (let [course-id (:id path-params)
        user-id (:id user)]
    (if (courses/course-by-user ds user-id course-id)
      (if (courses/remove-course-from-user! ds user-id course-id)
        (res/response {:message "Successfully removed course from user!"})
        (-> (res/response {:message "couldnt remove this for some reason. something is wonky!"})
            (res/status 500)))
      (res/response {:message "This course is not yet assigned to the user!"}))))


(defn subscribable-courses
  {:summary "Get the courses that a user has not yet subscribed to!"
   :responses (api/success api/GetCoursesResponse)}
  [{:keys [ds user] :as _request}]
  (let [user-id (:id user)
        course-ids (->> (courses/courses-by-user ds user-id)
                        (set))]
    (res/response (->> (courses/get-all ds)
                       (remove #(contains? course-ids (:id %)))))))


(defn start-session!
  {:summary "Start a practice session for a given id (yay)!"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success api/StartSessionResponse)}
  [{:keys [ds path-params] :as _request}]
  (let [unit (units/get-unit ds (:id path-params))]
    (res/response (select-keys unit [:level :exercises]))))

(defn end-session!
  {:summary "End a practice session by posting back analytics data!"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success [:map [:message :string]])}
  [_request]
  (res/response {:message "success!"}))

(comment
  
  (require '[abantu.db.interface :as db])
  (def ds (db/ds :master))
  (db/find ds {:tname :courses
               :ret :*})
  
  (db/find ds {:tname :user-courses
               :ret :*})
  
  (db/delete! ds {:tname :user-courses
                  :where [:= :id 1]})
  
  ())

