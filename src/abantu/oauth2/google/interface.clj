(ns abantu.oauth2.google.interface
  (:require [abantu.oauth2.google.core :as google]
            [abantu.cache :as cache]))

(def ^:private auth-reqs-service (cache/create-cache))

(defn auth-uri []
  (google/-auth-uri auth-reqs-service))

(defn google-session-user [uuid params]
  (google/-google-session-user auth-reqs-service uuid params))

(defn google-user-email
  "Public interface to get user email from google"
  [access-token]
  (google/google-user-email access-token))

