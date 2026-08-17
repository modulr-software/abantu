(ns abantu.middleware.auth.interface
  (:require [abantu.middleware.auth.core :as auth]
            [abantu.middleware.auth.util :as util]))

(defn create-session [user]
  (auth/create-session user))

(defn validate-request [req]
  (auth/validate-request req))

(defn wrap-auth [handler]
  (auth/wrap-auth handler))