(ns abantu.email.gmail
  (:require [postal.core :as postal]
            [abantu.config :as conf]))

(defn postal-config []
  (let [email-username (conf/read-value :email :username)
        gmail-password (conf/read-value :email :password)]
    {:host "smtp.gmail.com"
     :user email-username
     :pass gmail-password
     :port 587
     :tls true}))

(defn send-email [{:keys [to subject body type] :as _opts}]
  (let [email-username (conf/read-value :email :username)]
    (-> (postal-config)
        (postal/send-message
         {:from email-username
          :to to
          :subject subject
          :body [{:type (str (namespace type) "/" (name type))
                  :content body}]}))))

(comment
  (require '[abantu.email.templates :as templates])

  (send-email {:to "keaganncollins@gmail.com"
               :subject "test email"
               :body "hi this is a test coming from source-be"
               :type :text/plain})

  (send-email {:to "keaganncollins@gmail.com"
               :subject "feed rejection template"
               :body (templates/feed-rejection {:creator-name "Keagan"
                                                :feed-title "Keagan's Mukbang Channel"
                                                :reason "too cringe frfr"})
               :type :text/html})

  (send-email {:to "keaganncollins@gmail.com"
              :subject "feed approval template"
              :body (templates/feed-approval {:creator-name "Keagan"
                                              :feed-title "Keagan's Mukbang Channel"
                                              :feed-id 2})
               :type :text/html})

  (send-email {:to "keaganncollins@gmail.com"
               :subject "admin reported problem"
               :type :text/html
               :body (templates/admin-reported-problem {:user-id 3
                                                        :user-email "keaganncollins@gmail.com"
                                                        :user-type "creator"
                                                        :message "no burger king foot lettuce :("})})

  ())
