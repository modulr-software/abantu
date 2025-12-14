(ns abantu.crypt
  (:require [buddy.core.codecs :as cod]
            [buddy.core.hash :as hash]
            [buddy.core.crypto :as crypto]))

(defn- gen-iv [password]
  (->
   (hash/sha1 password)
   (cod/bytes->hex)
   (subs 0 16)))

(defn- gen-key [password]
  (->
   (hash/sha256 password)
   (cod/bytes->hex)
   (subs 0 32)))

(defn hash-value [value password]
  (try
    (-> value
        (cod/str->bytes)
        (crypto/encrypt
         (gen-key password)
         (gen-iv password))
        (cod/bytes->hex))
    (catch Exception _ false)))

(defn verify [hashed password]
  (try
    (-> hashed
        (cod/hex->bytes)
        (crypto/decrypt
         (gen-key password)
         (gen-iv password))
        (cod/bytes->str))
    (catch Exception _ false)))

(comment 
  (verify (hash-value "12345" "test") "test"))

