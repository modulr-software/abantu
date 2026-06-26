(ns abantu.io.file
  (:require [buddy.core.codecs :as codecs]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell])
  (:import (java.util Base64)))

(def ^:private audio-prefix ".db/audio/")

(defn wav->flac [wav-path flac-path]
  (let [{:keys [out err exit]} (shell/sh "flac" wav-path "-o" flac-path)]
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

(defn save-audio-file! [f type base64]
  (let [extension (if type
                    (str "." type)
                    ".wav")]

    (save-base64 f extension base64)

    (when (= extension ".wav")
      (wav->flac
       (str audio-prefix f extension)
       (str audio-prefix f ".flac")))))

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

  (wav->flac ".db/audio/test.wav" ".db/audio/test.flac")

  ())
