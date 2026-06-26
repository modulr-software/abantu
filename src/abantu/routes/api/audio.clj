(ns abantu.routes.api.audio
  (:require [abantu.routes.openapi :as api]
            [abantu.io.file :as io]
            [ring.util.response :as res]))
;; TODO - save files here with a file type extension. follow the todo of get-blob for details.
;; if no input is specified for type, just use wav and compress the file to flac (do this as a third step not right now).
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
;; TODO - this needs to also take the file type
;; we look up the file locally by scannign .db/audio for id + "." + type
;; only if the file exists in the desired filetype do we actually return it as blob
;; otherwise no (404)
(defn get-blob
  {:summary "down a sound byte as blob to use on an exercise or question"
   :parameters (api/params :query [:map [:id :string]])
   :responses {200 {:description "a binary file blob"
                    :content {"audio/wav" {:schema [:string {:json-schema/format "binary"}]}}}}}
  [{:keys [params]}]
  (prn "params" params)
  (-> (str  ".db/audio/" (:id params))
      (clojure.java.io/input-stream)
      (res/response)
      (res/header "Content-Type" "audio/wav")))

(comment
  
  
  (clojure.java.io/input-stream (str (System/getenv "HOME") "/Developer/abantu/.db/audio/" "correct_tone"))
  :end)