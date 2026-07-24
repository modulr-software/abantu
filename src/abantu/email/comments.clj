(ns abantu.email.comments
  (:require [abantu.email.gmail :as gmail]
            [abantu.email.templates :as templates]
            [abantu.services.units :as units]
            [abantu.services.courses :as courses]
            [abantu.services.users :as users]))

(defn comment-on-exercise!
  "Looks up the exercise, unit, course, and course creator from the db,
  then sends an email to the course creator notifying them of the new comment."
  [ds {:keys [exercise-id comment-text]}]
  (let [exercise (units/get-exercise ds exercise-id)
        {:keys [unit-id]} exercise
        unit (units/get-unit ds unit-id)
        {:keys [course-id]} unit
        course (courses/get-course ds course-id)
        creator (:creator course)
        creator-id (:id creator)
        creator-user (users/get-user ds creator-id)]
    (gmail/send-email
     {:to (:email creator-user)
      :subject (str "New comment on exercise in \"" (:name course) "\"")
      :type :text/html
      :body (templates/exercise-comment-notification
             {:creator-name (:firstname creator-user)
              :course-name (:name course)
              :unit-id unit-id
              :unit-name (:name unit)
              :exercise-id exercise-id
              :comment-text comment-text})})))
