(ns abantu.password
  (:require [buddy.core.hash :as hash]
            [buddy.core.codecs :as codecs]))

(defn hash-password [password]
  (-> (hash/digest password :sha1)
      (codecs/bytes->hex))
  )

(defn verify-password [password hashed-password]
  (let [nh (hash-password password)]
    (= nh hashed-password)))

(comment 
  (hash-password "123"))
