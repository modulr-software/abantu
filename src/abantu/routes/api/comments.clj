(ns abantu.routes.api.comments
  (:require [ring.util.response :as res]
            [abantu.routes.openapi :as api]
            [abantu.services.comments :as comments]
            [abantu.services.units :as units]
            [abantu.util :as util]))

(defn get-all
  {:summary "Get all comments"
   :responses (api/success api/GetCommentsResponse)}
  [{:keys [ds] :as _request}]
  (res/response (comments/get-all ds)))

(defn create-comment
  {:summary "Create a new comment on an exercise"
   :parameters (api/params :body api/CreateCommentParam)
   :responses (-> (api/success api/GetCommentResponse)
                  (api/bad-request))}
  [{:keys [ds body user] :as _request}]
  (let [{:keys [exercise-id text]} body
        exercise (units/get-exercise ds exercise-id)]
    (if-not exercise
      (-> (res/response {:message "Exercise not found"})
          (res/status 404))
      (let [{:keys [unit-id course-id]} exercise
            comment (comments/save-comment!
                     ds {:exercise-id exercise-id
                         :unit-id unit-id
                         :course-id course-id
                         :text text
                         :user-id (:id user)
                         :timestamp (util/get-utc-timestamp-string)})]
        (-> (res/response comment)
            (res/status 201))))))

(defn resolve-comment
  {:summary "Mark a comment as resolved"
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/GetCommentResponse)
                  (api/bad-request (api/error)))}
  [{:keys [ds path-params] :as _request}]
  (let [{:keys [id]} path-params
        existing (comments/get-comment ds id)]
    (if-not existing
      (-> (res/response {:message "Comment not found"})
          (res/status 400))
      (res/response (comments/resolve-comment! ds id)))))
