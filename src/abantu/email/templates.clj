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
         :style "background-color: #0F172A; padding: 40px; text-align: center; color: white; font-size: 36px;"}
    "Source"]])

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
         :style "background-color: #0F172A; padding: 20px; text-align: center; color: white; font-size: 14px;"}
    "Copyright © 2025 | Wearesource"]])

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
