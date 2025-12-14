(ns abantu.middleware.auth.util
  (:require [buddy.sign.jwt :as jwt]
            [abantu.config :as conf]
            [clojure.string :as str]))

(defn auth-header [request]
  (or (get-in request [:headers "Authorization"])
      (get-in request [:headers "authorization"])
      (get-in request [:headers :Authorization])
      (get-in request [:headers :authorization])))

(defn auth-token [request]
  (when-let [auth-token (auth-header request)]
    (cond
      (some? auth-token)
      (when-let [token
                 (-> auth-token (str/split #"Bearer\s") last)]
        token)
      :else nil)))

(defn sign-jwt [payload]
  (jwt/encrypt payload (conf/read-value :supersecretkey)))

(defn verify-jwt [token]
  (try
    (jwt/decrypt token (conf/read-value :supersecretkey))
    (catch Exception e
      (println (.getMessage e))
      false)))

