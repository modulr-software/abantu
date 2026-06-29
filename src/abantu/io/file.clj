(ns abantu.io.file
  (:require [buddy.core.codecs :as codecs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import (java.util Base64)))

(def ^:private audio-prefix ".db/audio/")

(defn list-paths [dir-path]
  (->> (io/file dir-path)
       .listFiles
       (mapv #(.getPath %))))

(defn remove-extension [filename]
  (str/replace filename #"\.[^.]+$" ""))

(defn wav-header? [file-path]
  (with-open [is (java.io.FileInputStream. (io/file file-path))]
    (let [buffer (byte-array 12)]
      (when (= 12 (.read is buffer))
        (let [header (String. buffer 0 4 "US-ASCII")
              format (String. buffer 8 4 "US-ASCII")]
          (and (= "RIFF" header) (= "WAVE" format)))))))

(defn audio-path [id type]
  (str audio-prefix id (if type (str "." type) ".flac")))


(defn wav->flac 
  {:dev/debt "use clojure to remove the file manually instead of relying on flac to overwrite it"}
  [wav-path flac-path]
  (let [{:keys [out err exit]} (shell/sh "flac" "-f" wav-path "-o" flac-path)]
    (if (zero? exit)
      (do
        (println out)
        true)
      (do
        (println err)
        false))))

(defn base64-to-bytes [base64]
  (codecs/b64->bytes base64))

(defn write-bytes [f bytes]
  (io/copy bytes (io/file (str audio-prefix f))))

(defn save-base64 [f extension base64]
  (->> base64
       (base64-to-bytes)
       (write-bytes (str f extension))))

(defn read-base64 [f]
  (with-open [input (io/input-stream (str audio-prefix f))
              output (java.io.ByteArrayOutputStream.)]
    (io/copy input output)
    (codecs/bytes->b64-str (.toByteArray output))))

(comment
  (def base64 "aGVsbG8=")

  (base64-to-bytes "aGVsbG8=")

  (save-base64 "test-out" "txt" base64)
  (spit "resources/test-out.txt" (base64-to-bytes base64))

  (slurp "resources/test-out.txt")

  (read-base64 "test-out.txt")
  (codecs/str->bytes base64)
  (= (read-base64 "test-out.txt") base64)

  (wav->flac ".db/audio/cheese.wav" ".db/audio/cheese.flac")
  (wav-header? ".db/audio/person")

  (list-paths ".db/audio")

  ())
