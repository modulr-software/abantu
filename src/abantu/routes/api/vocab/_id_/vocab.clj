(ns abantu.routes.api.vocab.-id-.vocab
  (:require [ring.util.response :as res]
            [abantu.services.interface :as services]))

(defn get
  [{:keys [ds path-params] :as _request}]
  (res/response (services/vocab-by-id ds {:id (:id path-params)})))

(defn post
  [{:keys [ds path-params body] :as _request}]
  (services/update-vocab! ds {:where [:= :id (:id path-params)]
                              :data body})
  (res/response {:message "successfully updated vocabulary"}))

(defn delete
  [{:keys [ds path-params] :as _request}]
  (let [id (:id path-params)]
    (if (some? id)
      (do
        (services/delete-vocab! ds {:id (:id path-params)})
        (res/response {:message "successfully deleted vocabulary"}))
      (res/response {:message "missing vocabulary id"}))))
