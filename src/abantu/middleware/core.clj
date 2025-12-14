(ns abantu.middleware.core
  (:require [abantu.middleware.content-type :as content-type]
            [abantu.config :as conf]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [ring.util.response :as res]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :as ring]
            [ring.middleware.cookies :as cookies]
            [clojure.walk :as walk]))

(defn wrap-ds [handler ds]
  (fn [request]
    (-> request
        (assoc :ds ds)
        (handler))))

(defn apply-ds [app ds]
  (-> app
      (wrap-ds ds)))

(defn wrap-exception-logger [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (println "Unhandled Exception:\n" e)
        (-> (res/response {:message "Internal Server Error"})
            (res/status 500))))))

(defn process-body [{:keys [body] :as req} t-fn]
  (assoc req
         :body
         (if
          (or (map? body) (vector? body) (seq? body))
           (cske/transform-keys (fn [k]
                                  (if (or (keyword? k) (string? k))
                                    (t-fn k)
                                    k)) body)
           body)))

(defn wrap-case-conversion [handler]
  (fn [request]
    (-> request
        (process-body csk/->kebab-case-keyword)
        (handler)
        (process-body csk/->camelCaseKeyword))))

(defn wrap-query [handler]
  (fn [{:keys [query-params] :as request}]
    (-> request
        (assoc :query-params (walk/keywordize-keys query-params))
        (handler))))

(defn apply-generic [app & {:keys [ds]}]
  (-> app
      (wrap-exception-logger)
      (apply-ds ds)
      (wrap-case-conversion)
      (wrap-query)
      (content-type/wrap-content-type)
      (wrap-cors :access-control-allow-origin [(re-pattern (conf/read-value :cors-origin))]
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-params)
      (wrap-defaults (assoc site-defaults :session false :security {:anti-forgery false}))
      (ring/wrap-json-response)
      (ring/wrap-json-body {:keywords? true})
      (cookies/wrap-cookies)))
