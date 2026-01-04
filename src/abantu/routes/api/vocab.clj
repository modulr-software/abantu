(ns abantu.routes.api.vocab
  (:require [abantu.services.interface :as services]
            [ring.util.response :as res]
            [clj-fuzzy.levenshtein :as levenshtein]
            [abantu.routes.openapi :as api]))


(defn get-all
  {:summary "get all the vocab n shit"
   :parameters (api/params :query api/VocabSearchParams) 
   :responses (api/success api/VocabSearchResponse)}
  [{:keys [ds query-params] :as _request}]
  (let [{:keys [type search]} query-params
        vocab (services/vocab-query ds {:type type})
        searched (if (some? search)
                   (->>
                    (mapv (fn [{:keys [xhosa english] :as v}]
                            (let [len (levenshtein/distance search english)
                                  lxh (levenshtein/distance search xhosa)]
                              (assoc v :levenshtein (if (< len lxh) len lxh)))) vocab)
                    (sort-by :levenshtein)
                    (mapv #(dissoc % :levenshtein)))
                   vocab)]
    (res/response searched)))

(defn add
  {:summary "insert all the vocab lol"
   :parameters (api/params :body api/InsertVocabParams)
   :responses (-> (api/success api/InsertVocabResponse)
                  (api/bad-request))}
  [{:keys [ds body] :as _request}]
  (let [new-word (services/insert-vocab! ds {:data body})]
    (-> (res/response new-word)
        (res/status 201))))


(defn get-one
  {:summary "Retrieve a single vocab record by id."
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/VocabByIdResult)
                  (api/not-found))}
  [{:keys [ds path-params] :as _request}]
  (let [vocab (services/vocab-by-id ds {:id (:id path-params)})]
    (if (some? vocab)
      (res/response vocab)
      (-> (res/response {:message "Vocab not found"})
          (res/status 404)))))

(defn update
  {:summary "Update a given vocab by id"
   :parameters (api/params :path api/IdPathParam
                           :body api/UpdateVocabBody)
   :responses (-> (api/success api/InsertVocabResponse)
                  (api/not-found))}
  [{:keys [ds path-params body] :as _request}]
  (services/update-vocab! ds {:where [:= :id (:id path-params)]
                              :data body})
  (res/response {:message "successfully updated vocabulary"}))

(defn delete
  {:summary "Delete a vocab by id"
   :parameters (api/params :path api/IdPathParam)
   :responses (-> (api/success api/DeleteVocabResponse))}
  [{:keys [ds path-params] :as _request}]
  (let [id (:id path-params)]
    (if (some? id)
      (do
        (services/delete-vocab! ds {:id (:id path-params)})
        (res/response {:message "successfully deleted vocabulary"}))
      (res/response {:message "missing vocabulary id"}))))