(ns abantu.email.templates
  (:require [hiccup.page :as h]
            [abantu.config :as conf]))

(defn head-metadata []
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]])

(defn header []
  [:tr
   [:td {:class "header"
         :style "background-color: #232523; padding: 40px; text-align: center; color: #7BF1A8; font-size: 36px;"}
    "ABANTU"]])

(defn button [{:keys [text redirect]}]
  [:tr
   [:td {:style "padding: 0px 40px 0px 40px; text-align: center;"}
    [:table {:cellspacing "0" :cellpadding "0" :style "margin: auto;"}
     [:tr
      [:td {:align "center"
            :style "background-color: #0F172A; padding: 10px 20px; border-radius: 5px;"}
       [:a {:href redirect
            :target "_blank"
            :style "color: #ffffff; text-decoration: none; font-weight: bold;"}
        text]]]]]])

(defn footer []
  [:tr
   [:td {:class "footer"
         :style "background-color: #232523; padding: 20px; text-align: center; color: #7BF1A8; font-size: 14px;"}
    "Copyright © 2026 | Abantu"]])

(defn feed-rejection
  "Returns the completed HTML for a feed rejection email"
  [{:keys [creator-name feed-title reason]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " creator-name)
          [:br]
          (str "Unfortunately, the feed \"" feed-title "\" that you recently added was rejected.")
          [:br] [:br]
          (str reason)
          [:br] [:br]
          "If you believe this was in error, you can reply to this email or click on the link below to leave us a message."]]
        (button {:text "Leave us a message"
                 :redirect (str (conf/read-cors-with-port) "/report-a-problem")})
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "Regards,"
          [:br]
          "The Source Team"]]
        (footer)]]]]]))

(defn feed-approval
  "Returns the completed HTML for a feed approval email"
  [{:keys [creator-name feed-title feed-id]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " creator-name)
          [:br]
          (str "Good news! The feed \"" feed-title "\" that you recently added was approved and is now live on the platform.")
          [:br] [:br]
          "Click on the link below to go to your dashboard and view your feed."]]
        (button {:text "View your feed"
                 :redirect (str (conf/read-cors-with-port) "/dashboard/feed/" feed-id)})
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "Regards,"
          [:br]
          "The Source Team"]]
        (footer)]]]]]))

(defn admin-reported-problem
  "Returns the completed HTML for an admin problem report email"
  [{:keys [user-id user-type user-email message]}]
  (let [shortened-message (if (> (count message) 15)
                            (str (subs message 0 15) "...")
                            message)]
    (h/html5
     {:lang "en"}
     (head-metadata)
     [:body {:style "font-family: 'Switzer', sans-serif"}
      [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
       [:tr
        [:td {:align "center" :style "padding: 20px;"}
         [:table {:class "content"
                  :width "600"
                  :border "0"
                  :cellspacing "0"
                  :cellpadding "0"
                  :style "border-collapse: collapse; border: 1px solid #cccccc;"}
          (header)
          [:tr
           [:td {:class "body"
                 :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
            "A user has reported a problem:"
            [:br] [:br]
            message
            [:br] [:br]
            (str "User ID: " user-id)
            [:br]
            (str "User email address: " user-email)
            [:br]
            (str "User type: " user-type)
            [:br]
            [:br]
            "Click on the link below to respond"]]
          (button {:text "Respond"
                   :redirect (str "mailto:" user-email "?subject=Source Team Re:" shortened-message)})
          [:tr
           [:td {:class "body"
                 :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
            "This is an automated message. Please do not reply directly to this email."]]
          (footer)]]]]])))

(defn creator-admission-request-acknowledgement
  "Returns the completed HTML for a creator admission request acknowledgement email"
  [{:keys [firstname]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " firstname ",")
          [:br] [:br]
          "Thank you for your interest in Abantu."
          [:br] [:br]
          "We have received your submission to become a Course Creator. Our Admin Team is reveiwing your request and will get back to you shortly."
          [:br] [:br]
          "Sincerely"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))

(defn creator-admission-request
  "Returns the completed HTML for a creator admission request email"
  [{:keys [email firstname lastname message]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "A user has requested admission to become a Course Creator:"
          [:br] [:br]
          message
          [:br] [:br]
          (str "User email address: " email)
          [:br]
          (str "First name: " firstname)
          [:br]
          (str "Last name: " lastname)
          [:br]]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]]]]]]))

(defn creator-approved
  "Returns the completed HTML for a creator approval email with a set password link"
  [{:keys [firstname set-password-url]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " firstname ",")
          [:br] [:br]
          "Your request to become a Course Creator on Abantu has been approved!"
          [:br] [:br]
          "Please set your password using the link below to access your account:"
          [:br] [:br]]]
        (button {:text "Set Your Password" :redirect set-password-url})
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          [:br]
          "Sincerely"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))

(defn exercise-comment-notification
  "Returns the completed HTML for a new exercise comment notification email.
  Sent to any editor of the course (including its creator), so the salutation
  and wording are recipient-neutral."
  [{:keys [recipient-name course-name unit-id unit-name exercise-id comment-text]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " recipient-name ",")
          [:br] [:br]
          (str "A new comment has been left on an exercise in the course \"" course-name "\".")
          [:br] [:br]
          (str "Unit: " unit-name)
          [:br]
          (str "Comment: \"" comment-text "\"")]]
        (button {:text "View Exercise"
                 :redirect (str (conf/read-cors-with-port) "/unit/" unit-id "/exercise/" exercise-id)})
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "Regards,"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))

(defn editor-added-notification
  "Returns the completed HTML for an editor-added notification email.
  Sent to a creator who has just been granted edit rights on a course."
  [{:keys [recipient-name course-name course-id]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " recipient-name ",")
          [:br] [:br]
          (str "You've been added as an editor of the course \"" course-name "\".")
          [:br] [:br]
          "You can now edit its units and exercises."]]
        (button {:text "View Course"
                 :redirect (str (conf/read-admin-portal-url) "/dashboard/courses/" course-id)})
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "Regards,"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))

(defn student-access-added-notification
  "Returns the completed HTML for a student-access-added notification email.
  Sent to a user who has just been granted access to a course as a student."
  [{:keys [recipient-name course-name course-id]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " recipient-name ",")
          [:br] [:br]
          (str "You've been given access to the course \"" course-name "\".")
          [:br] [:br]
          "You can now start learning its units and exercises."]]
        (button {:text "View Course"
                 :redirect (conf/read-app-url)})
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "Regards,"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))

(defn editor-removed-notification
  "Returns the completed HTML for an editor-removed notification email.
  Sent to a creator whose edit rights on a course have just been revoked."
  [{:keys [recipient-name course-name]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " recipient-name ",")
          [:br] [:br]
          (str "Your editor access to the course \"" course-name "\" has been revoked.")
          [:br] [:br]
          "You will no longer be able to edit its units and exercises."]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "Regards,"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))

(defn student-access-removed-notification
  "Returns the completed HTML for a student-access-removed notification email.
  Sent to a user whose access to a course has just been revoked."
  [{:keys [recipient-name course-name]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " recipient-name ",")
          [:br] [:br]
          (str "Your access to the course \"" course-name "\" has been revoked.")
          [:br] [:br]
          "You will no longer be able to access its units and exercises."]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          "Regards,"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))

(defn user-created
  "Returns the completed HTML for a new user set password email"
  [{:keys [firstname set-password-url]}]
  (h/html5
   {:lang "en"}
   (head-metadata)
   [:body {:style "font-family: 'Switzer', sans-serif"}
    [:table {:width "100%" :border "0" :cellspacing "0" :cellpadding "0"}
     [:tr
      [:td {:align "center" :style "padding: 20px;"}
       [:table {:class "content"
                :width "600"
                :border "0"
                :cellspacing "0"
                :cellpadding "0"
                :style "border-collapse: collapse; border: 1px solid #cccccc;"}
        (header)
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          (str "Hi " firstname ",")
          [:br] [:br]
          "An account has been created for you on Abantu."
          [:br] [:br]
          "Please set your password using the link below to access your account:"
          [:br] [:br]]]
        (button {:text "Set Your Password" :redirect set-password-url})
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 16px; line-height: 1.6;"}
          [:br]
          "Sincerely"
          [:br]
          "The Abantu Team"]]
        [:tr
         [:td {:class "body"
               :style "padding: 40px; text-align: left; font-size: 11px; line-height: 1.6;"}
          "This is an automated message. Please do not reply directly to this email."]]
        (footer)]]]]]))
