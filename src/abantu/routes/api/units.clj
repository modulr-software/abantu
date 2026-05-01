(ns abantu.routes.api.units
  (:require [ring.util.response :as res]
            [abantu.routes.openapi :as api]
            [abantu.services.units :as units]))

(defn get-units
  {:summary "get all units with the given course id"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success api/GetUnitsResponse)}
  [{:keys [ds path-params] :as _request}]
  (res/response (units/get-units ds (:id path-params))))

(defn create-units
  {:summary "Create new empty units"
   :parameters (api/params :path api/IdPathParam :body api/CreateUnitsParam)
   :responses (api/success api/CreateUnitsResponse)}
  [{:keys [body ds path-params user] :as _request}]
  (let [units (->> (mapv #(assoc % :creator-id (:id user) :course-id (:id path-params)) body)
                   (units/save-units! ds))]
    (res/response {:message "Successfully added units"
                   :data units})))

(defn get-by-id
  {:summary "get a unit by id"
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/GetUnitResponse)
                  (api/not-found (api/error))
                  (api/bad-request (api/error)))}
  [{:keys [ds path-params] :as _request}]
  (let [{:keys [id]} path-params]
    (res/response  (units/get-unit ds id))))

(defn update-unit
  {:summary "Update a unit with a given id"
   :parameters (api/params :path api/IdPathParam :body api/UpdateUnitParam)
   :responses (api/success api/UpdateUnitResponse)}
  [{:keys [body ds] :as _request}]
  (let [{:keys [id]} body
        _result (units/update-unit! ds body)]
    (res/response
     (units/get-unit ds id))))

(defn delete-unit
  {:summary "delete new empty units"
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success (api/response-schema))
                  (api/not-found (api/response-schema)))}
  [{:keys [ds path-params] :as _request}]
  (if (units/delete-unit! ds (:id path-params))
    (res/response {:message "Successfully deleted unit"})
    (-> (res/response "not found")
        (res/status 404))))

(defn get-exercises-for-unit
  {:summary "Get all exercises for a unit with given id"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success api/GetExercisesResponse)}
  [{:keys [ds path-params] :as _request}]
  (res/response (units/get-exercises-for-unit ds (:id path-params))))

(defn add-exercises-to-unit
  {:summary "Add exercises to a unit with given id"
   :parameters (api/params :path api/IdPathParam
                           :body api/ExerciseParams)
   :responses (api/success (api/response-schema))}
  [{:keys [ds path-params body] :as _request}]
  (prn "body" body)
  (let [{:keys [id course-id]} (units/get-unit ds (:id path-params))]
    (units/save-exercises! ds (mapv #(assoc % :unit-id id :course-id course-id) body))
    (res/response {:message "successfully added exercises to unit"})))

(defn get-exercise
  {:summary "Get an exercises by a given id"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success api/GetExerciseResult)}
  [{:keys [ds path-params] :as _request}]
  (res/response (units/get-exercise ds (:id path-params))))

(defn update-exercise
  {:summary "Update an exercises by a given id"
   :parameters (api/params :path api/IdPathParam
                           :body api/UpdateExerciseParam)
   :responses (api/success api/GetExerciseResult)}
  [{:keys [ds path-params body] :as _request}]
  (units/update-exercise! ds (assoc body :id (:id path-params)))
  (res/response {:message "Successfully updated exercise"}))

(defn delete-exercise
  {:summary "Delete an exercise by a given id"
   :parameters (api/params :path api/IdPathParam)
   :responses (api/success (api/response-schema))}

  [{:keys [ds path-params] :as _reqyuest}]
  (units/delete-exercise ds (:id path-params))
  (res/response {:message "Successfully deleted exercise"}))

