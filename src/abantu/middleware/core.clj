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
            [ring.middleware.multipart-params :as multipart-params]
            [clojure.walk :as walk]
            [abantu.util :as util]
            [clojure.string :as string]))

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

(defn process-query-params [{:keys [query-params] :as req} t-fn]
  (assoc req
         :query-params
         (if
          (or (map? query-params) (vector? query-params) (seq? query-params))
           (cske/transform-keys (fn [k]
                                  (if (or (keyword? k) (string? k))
                                    (t-fn k)
                                    k)) query-params)
           query-params)))

(defn wrap-case-conversion [handler]
  (fn [request]
    (-> request
        (process-body csk/->kebab-case-keyword)
        (process-query-params csk/->kebab-case-keyword)
        (handler)
        (process-body csk/->camelCaseKeyword))))

(defn wrap-query [handler]
  (fn [{:keys [query-params] :as request}]
    (-> request
        (assoc :query-params (walk/keywordize-keys query-params))
        (handler))))

(defn- validate-param [request [param-type schema]]
  (let [{:keys [error] :as validated} (-> (cond
                                            (= param-type :body) (:body request)
                                            (= param-type :path) (:path-params request)
                                            (= param-type :query) (:query-params request))
                                          (util/validate schema))]
    (->> (when error
           (str "In " (name param-type) ":\n" error))
         (assoc validated :error))))

(defn wrap-input-validation [handler openapi-meta]
  (fn [request]
    (let [errors (->> (mapv (partial validate-param request) (:parameters openapi-meta))
                      (filter #(:error %))
                      (mapv :error))]
      (if (seq errors)
        (-> (res/response {:message (string/join "\n" errors)})
            (res/status 400))
        (handler request)))))

(defn apply-generic [app & {:keys [ds]}]
  (-> app
      (wrap-exception-logger)
      (apply-ds ds)
      (wrap-case-conversion)
      (wrap-query)
      (content-type/wrap-content-type)
      (wrap-cors :access-control-allow-origin [(re-pattern (conf/read-cors-with-port))]
                 :access-control-allow-methods [:get :put :post :delete])
      (wrap-params)
      (wrap-defaults (assoc site-defaults :session false :security {:anti-forgery false}))
      (ring/wrap-json-response)
      (ring/wrap-json-body {:keywords? true})
      (cookies/wrap-cookies)))
