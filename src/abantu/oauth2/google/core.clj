(ns abantu.oauth2.google.core
  (:require [abantu.config :as conf]
            [abantu.util :as util]
            [abantu.cache :as cache]
            [clojure.data.json :as json]
            [clj-oauth2.client :as oauth2]))

(defn auth-request [access-token]
  {:headers {"Authorization" (str "Bearer " access-token)}})

(defn user-info [auth-request]
  (let [body (->
              (oauth2/get
               "https://www.googleapis.com/oauth2/v1/userinfo"
               auth-request)
              (:body))]
    (if body
      (:email (json/read-json body {:key-fn keyword}))
      nil)))

(defn google-user-email [access-token]
  (-> access-token
      (auth-request)
      (user-info)))

(defn -auth-uri [auth-reqs-service]
  (let [req (cache/add-item auth-reqs-service
                            (oauth2/make-auth-request
                             (conf/read-value :oauth2 :google)))]
    {:uri (get-in req [:item :uri])
     :uuid (:uuid req)}))

(defn -google-session-user [auth-reqs-service uuid params]
  (let [result (-> (conf/read-value :oauth2 :google)
                   (oauth2/get-access-token
                    params
                    (cache/get-item auth-reqs-service uuid)))]
    (cache/remove-item auth-reqs-service uuid)
    (google-user-email (:access-token result))))

(comment
  (def uuid (util/uuid)))

