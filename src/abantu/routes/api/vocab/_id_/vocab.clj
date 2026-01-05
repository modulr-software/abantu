(ns abantu.routes.api.vocab.-id-.vocab
  (:require [ring.util.response :as ring]
            [abantu.services.interface :as services]
            [abantu.routes.openapi :as api]))

(defn get
  {:summary "Retrieve a single vocab record by id."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/VocabByIdResult)
                  (api/not-found))}
  [{:keys [ds path-params] :as _request}]
  (let [vocab (services/vocab-by-id ds {:id (:id path-params)})]
    (if (some? vocab)
      (ring/response vocab)
      (-> (ring/response {:message "Vocab not found"})
          (ring/status 404)))))

(defn post
  {:summary "Update a given vocab by id"
   :parameters (api/params :path api/IdPathParam
                           :body api/UpdateVocabBody)
   :responses (-> (api/success api/InsertVocabResponse)
                  (api/not-found))}
  [{:keys [ds path-params body] :as _request}]
  (services/update-vocab! ds {:where [:= :id (:id path-params)]
                              :data body})
  (ring/response {:message "successfully updated vocabulary"}))

(defn delete
  {:summary "Delete a vocab by id"
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/DeleteVocabResponse))}
  [{:keys [ds path-params] :as _request}]
  (let [id (:id path-params)]
    (if (some? id)
      (do
        (services/delete-vocab! ds {:id (:id path-params)})
        (ring/response {:message "successfully deleted vocabulary"}))
      (ring/response {:message "missing vocabulary id"}))))
