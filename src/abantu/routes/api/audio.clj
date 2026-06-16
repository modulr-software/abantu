(ns abantu.routes.api.audio
  (:require [abantu.routes.openapi :as api]
            [abantu.io.file :as io]
            [ring.util.response :as res]))

(defn upload-audio
  {:summary "Upload a sound byte to use on an exercise or question"
   :parameters (api/params :body [:vector [:map [:audio :string] [:id :string] [:type {:optional true} :string]]]) 
   :responses (api/success [:map [:message :string]])}
  [{:keys [body]}]
  (run! #(io/save-base64 (:id %) (:audio %)) body)
  (res/response {:message "successfully uploaded audio!"}))

(defn get-audio
  {:summary "download a sound byte as base64 to use on an exercise or question"
   :parameters (api/params :query [:map [:id :string]])
   :responses (api/success [:map [:audio :string]])}
  [{:keys [params]}]
  (try
    (res/response {:audio (io/read-base64 (:id params))})
    (catch Exception _
      (-> (res/response {:audio nil})
          (res/status 404)))))