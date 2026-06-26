(ns abantu.migrations.002-convert-to-flac
  (:require [abantu.io.file :as io]
            [clojure.string :as str]))

(defn run-up! [_context]
  (let [files (io/list-paths ".db/audio")]
    (run! (fn [f]
            (when (io/wav-header? f)
              (let [o (str
                       (if (str/includes? f ".wav")
                         (io/remove-extension f)
                         f)
                       ".flac")]
                (io/wav->flac f o))))
          files)))

(defn run-down! [_context])
