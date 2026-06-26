(ns abantu.routes.api.audio
  (:require [abantu.routes.openapi :as api]
            [abantu.io.file :as io]
            [ring.util.response :as res]
            [clojure.java.io :as java.io]))

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
  (run! (fn [{:keys [audio id type]}]
          (let [extension (if type (str "." type) ".wav")]
            (io/save-base64 id extension audio)
            (when (= extension ".wav")
              (io/wav->flac
               (io/audio-path id extension)
               (io/audio-path id "flac"))))) body)

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
                    :content {"audio/flac" {:schema [:string {:json-schema/format "binary"}]}}}
               404 {:body [:map [:message :string]]}}}
  [{:keys [params]}]

  (let [{:keys [id type]} params
        filepath (io/audio-path id type)]
    (if (.exists (java.io/file filepath))
      (-> filepath
          (java.io/input-stream)
          (res/response)
          (res/header "Content-Type" (if type (str "audio/" type) "audio/flac")))

      (-> (res/response {:message (str "The audio file at '" filepath "' does not exist.")})
          (res/status 404)))))

(comment
  (java.io/input-stream (str (System/getenv "HOME") "/Developer/abantu/.db/audio/" "correct_tone"))
  :end)
