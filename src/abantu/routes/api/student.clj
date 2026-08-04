(ns abantu.routes.api.student
  (:require [ring.util.response :as res]
            [abantu.services.courses :as courses]
            [abantu.routes.openapi :as api]
            [abantu.services.units :as units]
            [abantu.services.sessions :as sessions]
            [abantu.services.stats :as stats]
            [abantu.db.util :as db.util]))

(defn- with-progress [user-id unit]
  (assoc unit :progress (stats/unit-progress user-id (:id unit))))

(defn- with-course-progress [course]
  (assoc course :overall-progress (stats/course-progress (:units course))))

(defn get-courses
  {:summary "Get all courses a student has started for a given student id"
   :responses (-> (api/success api/GetCoursesResponse))}
  [{:keys [ds user] :as _request}]
  (let [user-id (:id user)]
    (res/response
     (mapv (comp with-course-progress
                 #(update % :units (fn [us] (mapv (partial with-progress user-id) us))))
           (courses/courses-by-user ds user-id)))))

(defn get-course
  {:summary "Get course progress info for a given course with units"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success api/GetCourseResponse)}
  [{:keys [ds user path-params] :as _request}]
  (let [{:keys [id]} path-params
        user-id (:id user)]
    (res/response
     (some-> (courses/course-by-user ds user-id id)
             (update :units (fn [us] (mapv (partial with-progress user-id) us)))
             with-course-progress))))

(defn assign-course!
  {:summary "Subscribe the current user to a course. Only publishable, visible
             courses may be self-subscribed to."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success [:map [:message :string]])
                  (api/response 403 (api/error)))}
  [{:keys [ds user path-params] :as _request}]
  (let [course-id (:id path-params)
        user-id (:id user)
        course (courses/get-course ds course-id)]
    (if (and (courses/public? course) (:visible course))
      (if-let [_user-course (courses/assign-course-to-user! ds user-id course-id)]
        (res/response {:message "Test response"})
        (res/response {:message "nein! this course is already assigned to the user."}))
      (-> (res/response {:message "Forbidden: this course is not available for subscription."})
          (res/status 403)))))

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
  {:summary "Get the courses that a user has not yet subscribed to. Only publishable,
             visible courses are returned."
   :responses (api/success api/GetCoursesResponse)}
  [{:keys [ds user] :as _request}]
  (let [user-id (:id user)
        course-ids (->> (courses/courses-by-user ds user-id)
                        (map :id)
                        (set))
        public-courses (->> (courses/get-all ds)
                            (filter #(and (courses/public? %) (:visible %))))
        mod (remove #(contains? course-ids (:id %)) public-courses)]
    (res/response mod)))

(defn start-session!
  {:summary "Start a practice session for a given unit id (yay)!"
   :parameters (api/params :body api/StartSessionParams)
   :responses (api/success api/StartSessionResponse)}
  [{:keys [ds body user] :as _request}]
  (with-open [student-ds (db.util/conn :student (:id user))]
    (let [unit (units/get-unit ds (:unit-id body))
          session (sessions/start-session! student-ds unit)]
      (res/response (assoc (select-keys unit [:level :exercises])
                           :session-id (:id session))))))

(defn end-session!
  {:summary "End a practice session by posting back analytics data!"
   :parameters (api/params :body api/EndSessionParams)
   :responses (-> (api/success api/EndSessionResponse)
                  (api/response 404 (api/error)))}
  [{:keys [body user] :as _req}]
  (with-open [student-ds (db.util/conn :student (:id user))]
    (let [{:keys [session-id answers]} body
          session (sessions/get-session student-ds session-id)]
      (if-not session
        (-> (res/response {:message "Session not found"})
            (res/status 404))
        (do
          (sessions/end-session! student-ds session answers)
          (res/response
           (merge {:new-progress (stats/unit-progress (:id user) (:unit-id session))}
                  (stats/session-delta (:id user) session-id))))))))

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

