(ns abantu.routes.api.units.-id-.unit
  (:require [abantu.services.interface :as services]
            [ring.util.response :as res]
            [abantu.routes.openapi :as api]
            [abantu.db.interface :as db]
            [abantu.services.units :as units]))


(defn get
  {:summary "get a unit by id"
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/GetUnitResponse)
                  (api/not-found (api/error))
                  (api/bad-request (api/error)))}
  [{:keys [ds path-params] :as _request}]
  (let [{:keys [id]} path-params]
    (res/response  (units/get-unit ds id))))

(defn post
  {:summary "Update a unit with a given id"
   :parameters (api/params :body api/CreateUnitParams)
   :responses (api/success api/CreateUnitsResponse)}
   [{:keys [body ds] :as _request}]
  (res/response {:message "Successfully added units"
                 :data (units/save-units! ds body)}))

