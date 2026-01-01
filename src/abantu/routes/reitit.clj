(ns abantu.routes.reitit
  (:require [reitit.ring :as ring]
            [abantu.middleware.interface :as mw]
            [abantu.db.interface :as db]
            [abantu.routes.file :as file]
            [reitit.coercion.malli]
            [malli.util :as mu]
            [abantu.routes.api.vocab.vocabs :as vocabs]
            [abantu.routes.api.vocab.-id-.vocab :as vocab]
            [abantu.routes.api.units.units :as units]
            [abantu.routes.api.units.-id-.unit :as unit]
            [abantu.routes.api.units.-id-.exercises.manage.exercises :as exercises]
            [abantu.routes.api.units.-id-.exercises.manage.-id-.exercise :as exercise]
            [abantu.routes.api.units.-id-.exercises.generate.exercises-generate :as exercises-generate]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.openapi :as openapi]
            [abantu.util :as util]))

(defn route [handlers]
  (reduce (fn [acc [k v]]
            (let [{:keys [middleware summary parameters responses]} (util/metadata v)]
              (merge acc {k {:middleware middleware
                             :summary summary
                             :parameters parameters
                             :responses responses
                             :handler v}})))
          {} handlers))

(defn create-app
  ([] (create-app {:ds (db/ds :master)}))
  ([{:keys [ds]}]
   (let [ds (or ds (db/ds :master))]
     (ring/ring-handler
      (ring/router
       [["/swagger.json"   {:get {:no-doc true
                                  :swagger {:info {:title "source-api"
                                                   :description "swagger docs for source api with malli and reitit-ring"
                                                   :version "0.0.1"}
                                            :securityDefinitions {"auth" {:type :apiKey
                                                                          :in :header
                                                                          :name "Authorization"}
                                                                  "apiKey" {:type :apiKey
                                                                            :in :header
                                                                            :name "Authorization"}}}
                                  :handler (swagger/create-swagger-handler)}}]

        ["/openapi.json"   {:get {:no-doc true
                                  :openapi {:info {:title "source-api"
                                                   :description "openapi3 docs for source api with malli and reitit-ring"
                                                   :version "0.0.1"}
                                            :components {:securitySchemes {"bearerAuth" {:type :http
                                                                                         :scheme :bearer
                                                                                         :bearerFormat "JWT"
                                                                                         :description "JWT Authorization using the Bearer scheme"}
                                                                           "apiKey" {:type :http
                                                                                     :scheme :bearer
                                                                                     :description "API Key authorization using the Bearer scheme"}}}}
                                  :handler (openapi/create-openapi-handler)}}]
        ["/"              {:middleware [[mw/apply-generic :ds ds]]}
         ;;["test"              (fn [_request] {:status 200 :body {:message "success"}})]

         ["api"
          ["/vocab"
           [""            (route {:get vocabs/get
                                  :post vocabs/post})]
           ["/:id" (route {:get vocab/get
                           :post vocab/post
                           :delete vocab/delete})]]]]]

       {:data {:coercion (reitit.coercion.malli/create
                          {:error-keys #{#_:type :coercion :in :schema :value :errors :humanized #_:transformed}
                           :compile mu/closed-schema
                           :strip-extra-keys true
                           :default-values true
                           :options nil})
               :middleware [[mw/apply-generic :ds ds]]}})
      (ring/routes
       (swagger-ui/create-swagger-ui-handler {:path "/"
                                              :config {:validatorUrl nil
                                                       :urls [{:name "swagger", :url "swagger.json"}
                                                              {:name "openapi", :url "openapi.json"}]
                                                       :urls.primaryName "swagger"
                                                       :operationsSorter "alpha"}})
       (ring/create-default-handler))))))

(comment
  ())
