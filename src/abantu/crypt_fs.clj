(ns abantu.crypt-fs
  (:require
   [abantu.crypt :as crypt]))

(defn write-file-crypt! [path password content]
  (let [ciphertext (crypt/hash-value content password)]
    (spit path ciphertext)))

(defn read-file-crypt [path password]
  (-> (slurp path)
      (crypt/verify password)))

