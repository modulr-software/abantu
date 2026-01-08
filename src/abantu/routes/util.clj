(ns abantu.routes.util
  (:require [abantu.util :as util]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.openapi :as openapi]
            [reitit.coercion.malli :as coercion]
            [abantu.middleware.interface :as mw]
            [malli.util :as mu]
            [ring.util.response :as res]))

(defn- extract-openapi-meta [handler]
  (-> (util/metadata handler)
      (dissoc :arglists :line :column :file :name :ns)))

(defn- validate-param [request [param-type schema]]
  (-> (cond
        (= param-type :body) (:body request)
        (= param-type :path) (:path-params request)
        (= param-type :query) (:query-params request))
      (util/validate schema)))

(defn- attach-handler [handler openapi-meta]
  (if (some? (:parameters openapi-meta))
    (assoc openapi-meta
           :handler
           (fn [request]
             (let [errors (->> (mapv (partial validate-param request) (:parameters openapi-meta))
                               (filter #(:error %))
                               (mapv :error))]
               (if (seq errors)
                 (-> (res/response errors)
                     (res/status 400))
                 (handler request))))
           :responses (merge (:responses openapi-meta)
                             {400 {:body [:vector
                                          [:map
                                           [:input [:map-of :keyword :any]]
                                           [:errors [:or [:map-of :keyword [:vector :string]] [:vector :string]]]]]}}))
    (assoc openapi-meta :handler handler)))

(defn- merge-route-map [acc [method handler]]
  (->> (extract-openapi-meta handler)
       (attach-handler handler)
       (assoc {} method)
       (merge acc)))

(defn- parse-route-opts [& opts]
  (let [map-first? (map? (first opts))
        route-map (if map-first? (first opts) {})
        opts (if map-first? (rest opts) opts)]
    [route-map (vec opts)]))

(defn route
  [& opts]
  (let [[route-map opts] (apply parse-route-opts opts)]
    (->> (partition 2 opts)
         (reduce merge-route-map {})
         (merge route-map))))

(defn- resolve-route-map [method]
  (fn
    ([handler] (route {} method handler))
    ([route-map handler]
     (when (not (map? route-map))
       (throw (ex-info "Invalid argument for resolve-route-map: route-map must be a map"
                       {:panic? "not really"})))
     (route route-map method handler))))

(defn get [& opts]
  (apply (resolve-route-map :get) (vec opts)))

(defn post [& opts]
  (apply (resolve-route-map :post) (vec opts)))

(defn delete [& opts]
  (apply (resolve-route-map :delete) (vec opts)))

(defn swagger-ui-handler []
  (swagger-ui/create-swagger-ui-handler {:path "/"
                                         :config {:validatorUrl nil
                                                  :urls [{:name "swagger", :url "swagger.json"}
                                                         {:name "openapi", :url "openapi.json"}]
                                                  :urls.primaryName "swagger"
                                                  :operationsSorter "alpha"}}))

(defn swagger-route []
  ["/swagger.json" {:get {:no-doc true
                          :swagger {:info {:title "abantu-api"
                                           :description "swagger docs for abantu source api with malli and reitit-ring"
                                           :version "0.0.1"}
                                    :securityDefinitions {"auth" {:type :apiKey
                                                                  :in :header
                                                                  :name "Authorization"}
                                                          "apiKey" {:type :apiKey
                                                                    :in :header
                                                                    :name "Authorization"}}}
                          :handler (swagger/create-swagger-handler)}}])

(defn openapi-route []
  ["/openapi.json" {:get {:no-doc true
                          :openapi {:info {:title "abantu-api"
                                           :description "openapi3 docs for abant api with malli and reitit-ring"
                                           :version "0.0.1"}
                                    :components {:securitySchemes {"bearerAuth" {:type :http
                                                                                 :scheme :bearer
                                                                                 :bearerFormat "JWT"
                                                                                 :description "JWT Authorization using the Bearer scheme"}
                                                                   "apiKey" {:type :http
                                                                             :scheme :bearer
                                                                             :description "API Key authorization using the Bearer scheme"}}}}
                          :handler (openapi/create-openapi-handler)}}])

(defn data-map [ds]
  {:data {:coercion (coercion/create
                     {:error-keys #{#_:type :coercion :in :schema :value :errors :humanized #_:transformed}
                      :compile mu/closed-schema
                      :strip-extra-keys true
                      :default-values true
                      :options nil})
          :middleware [[mw/apply-generic :ds ds]]}})
