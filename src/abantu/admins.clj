(ns abantu.admins
  (:require [abantu.crypt-fs :as crypt]
            [abantu.config :as conf]
            [clojure.data.json :as json]))

(defn encrypt! []
  (crypt/write-file-crypt! (conf/read-value :admins-encrypted-path)
                           (conf/read-value :supersecretkey)
                           (slurp (conf/read-value :admins-path))))

(defn read
  "reads admin user information from file"
  []
  (try
    (-> (conf/read-value :admins-encrypted-path)
        (crypt/read-file-crypt (conf/read-value :supersecretkey))
        (json/read-json))
    (catch Exception e
      (println (str "Couldn't read the admins file: " (.getMessage e)))
      [])))
