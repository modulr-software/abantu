(ns abantu.routes.api.audio
  (:require [abantu.routes.openapi :as api]
            [abantu.io.file :as io]
            [ring.util.response :as res]
            ;; TODO - this can just be java-io
            [clojure.java.io :as jio]))

(defn upload-audio
  {:summary "Upload a sound byte to use on an exercise or question"
   :parameters (api/params
                :body [:vector
                       [:map
                        [:audio :string]
                        [:id :string]
                        [:type {:optional true} :string]]])
   :responses (api/success [:map [:message :string]])}
  [{:keys [body]}]
  (run! #(io/save-audio-file! (:id %) (:type %) (:audio %)) body)
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


(defn get-blob
  {:summary "down a sound byte as blob to use on an exercise or question"
   :parameters (api/params :query [:map
                                   [:id :string]
                                   [:type {:optional true} :string]])
   :responses {200 {:description "a binary file blob"
                    :content {"audio/wav" {:schema [:string {:json-schema/format "binary"}]}}}
               404 {:body [:map [:message :string]]}}}
  [{:keys [params]}]

  (let [{:keys [id type]} params
        ;; TODO - this needs to be a helper function in io/file.clj
        filepath (str ".db/audio/" id (when type (str "." type)))]
    (if (.exists (jio/file filepath))
      (-> filepath
          (jio/input-stream)
          (res/response)
          ;; TODO - this type needs to reflect what is being fetched
          (res/header "Content-Type" "audio/wav"))

      (-> (res/response {:message (str "The audio file at '" filepath "' does not exist.")})
          (res/status 404)))))

(comment
  (jio/input-stream (str (System/getenv "HOME") "/Developer/abantu/.db/audio/" "correct_tone"))
  :end)
