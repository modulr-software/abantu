(ns abantu.routes.api.units.units
  (:require [ring.util.response :as res]
            [abantu.routes.openapi :as api]
            [abantu.services.units :as units]))

(defn get
  {:summary "get all units"
   :responses (api/success api/GetUnitsResponse)}
  [{:keys [ds] :as _request}]
  (res/response (units/get-all-units ds)))

(defn post
  {:summary "Create new empty units"
   :parameters (api/params :body api/CreateUnitParams)
   :responses (api/success api/CreateUnitsResponse)}
   [{:keys [body ds] :as _request}]
  (res/response {:message "Successfully added units"
                 :data (units/save-units! ds body)}))

(comment
(api/params :body api/CreateUnitParams)
(api/success api/CreateUnitsResponse)
  ())
