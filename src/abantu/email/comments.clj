(ns abantu.email.comments
  (:require [abantu.async.interface :as async]
            [abantu.email.templates :as templates]
            [abantu.services.units :as units]
            [abantu.services.courses :as courses]
            [abantu.services.users :as users]))

(defn- notifyable? [creator-admin? recipient]
  (and (some? recipient)
       (or creator-admin?
           (not= "admin" (:role recipient)))))

(defn comment-on-exercise!
  "Notifies every editor of the course (plus its creator) about a new comment
  on an exercise. Admin recipients are excluded unless the course was itself
  created by an admin. Each recipient gets a personalized fire-and-forget
  email via async/handle-command :send-email."
  [ds {:keys [exercise-id comment-text]}]
  (let [exercise       (units/get-exercise ds exercise-id)
        unit           (units/get-unit ds (:unit-id exercise))
        course         (courses/get-course ds (:course-id unit))
        creator        (:creator course)
        creator-admin? (= "admin" (:role creator))
        recipients     (filter (partial notifyable? creator-admin?)
                               (cons creator
                                     (courses/editors-by-course ds (:id course))))]
    (doseq [r recipients]
      (async/handle-command :send-email
                            {:to (:email r)
                             :subject (str "New comment on exercise in \"" (:name course) "\"")
                             :type :text/html
                             :body (templates/exercise-comment-notification
                                    {:recipient-name (:firstname r)
                                     :course-name (:name course)
                                     :unit-id (:id unit)
                                     :unit-name (:name unit)
                                     :exercise-id exercise-id
                                     :comment-text comment-text})}))))
