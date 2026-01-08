(ns abantu.util
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.main :refer [demunge]]
            [malli.core :as m]
            [malli.transform :as mt]
            [clojure.string :as string])
  (:import (java.math BigInteger)
           (java.security MessageDigest)))

(defn vectors?
  "Returns true if coll is a 2d vector"
  [coll]
  (and (vector? coll)
       (vector? (first coll))))

(defn content-type [request]
  (or (get-in request [:headers "Content-Type"])
      (get-in request [:headers :content-type])
      (get-in request [:headers "content-type"])
      (get-in request [:headers :Content-Type])))

(defn unwrap-keys [m]
  (if (some? m)
    (update-keys m (fn [k] (keyword (name k)))) nil))

(defn prr [value]
  (println value)
  value)

(defn get-utc-timestamp-string []
  (let [formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ss'Z'")
        utc-now (java.time.ZonedDateTime/now java.time.ZoneOffset/UTC)]
    (.format formatter utc-now)))

(defn uuid []
  (->
   (nonce/random-bytes 8)
   (codecs/bytes->hex)))

(defn metadata [func]
  (-> (class func)
      (print-str)
      (demunge)
      (symbol)
      (find-var)
      (meta)))

(defn sha256
  "Computes SHA256 hash of given string and returns it as a hex string"
  [s]
  (let [digest (MessageDigest/getInstance "SHA-256")
        bytes (.getBytes s "UTF-8")
        hash-bytes (.digest digest bytes)]
    (format "%064x" (BigInteger. 1 hash-bytes))))

(defn parse-type-from-schema [schema]
  (cond
    (keyword? schema)
    (name schema)

    (and (seq schema) (= (first schema) :vector))
    (try
      (str "Array[" (string/join " " (mapv name (rest schema))) "]")
      (catch Exception _ "Array[]"))

    (and (seq schema) (= (first schema) :map))
    (try
      (str "Object { " (reduce (fn [acc pair]
                                 (str acc (string/join ": " (mapv name pair)) "; "))
                               "" (rest schema)) "}")
      (catch Exception _ "Object {}"))))

(defn append-humanised-error [acc error-map]
  (let [path (string/join "." (mapv (fn [p]
                                      (if (keyword? p)
                                        (name p)
                                        (str "[" p "]"))) (:path error-map)))
        k (if (or (nil? path) (= path "")) "" (str path ": "))
        value (:value error-map)
        schema (m/form (:schema error-map))
        error-type (cond
                     (= (:type error-map) :malli.core/missing-key) :missing
                     (= (:type error-map) :malli.core/invalid-type) :invalid
                     :else :type-error)
        expected (parse-type-from-schema schema)]
    (cond
      (= error-type :missing) (str acc "Missing required key: '" path "'\n")
      (= error-type :invalid) (str acc k "Expected '" expected "', found '" value "'\n")
      :else (str acc k "Expected '" expected "', found '" value "'\n"))))

(defn humanise [{:keys [errors] :as _error}]
  (reduce append-humanised-error "" errors))

(defn validate
  [data schema]
  (let [transformed (m/decode schema data mt/string-transformer)
        success (m/validate schema transformed)]
    {:data (when success transformed)
     :success success
     :error (when-not success (->> transformed
                                   (m/explain schema)
                                   (humanise)))}))

(defn format-rss-date
  "Takes a date as a string in RFC 1123 format and returns it in a format that meets ISO 8601 standards for SQLite.
  This is necessary because some RSS feeds use a different date than what is accepted by SQLite.
  Returns the original string if it is not in this format."
  [s]
  (try
    (let [zdt (java.time.ZonedDateTime/parse s java.time.format.DateTimeFormatter/RFC_1123_DATE_TIME)
          zdt-utc (.withZoneSameInstant zdt (java.time.ZoneId/of "UTC"))
          out (.format zdt-utc (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))]
      out)
    (catch Exception _ s)))

(comment
  (require '[abantu.routes.business :as business])
  (require '[abantu.routes.analytics.creator.top :as ctop])
  (validate business/post {:cheese "modulr"})
  (sha256 "1")

  (validate {:message "yeet"
             :b 1
             :array {}
             :test []
             :units [{:name "cheese"}]}
            [:map
             [:message :string]
             [:a :int]
             [:b :string]
             [:array [:vector :int]]
             [:test [:map [:a :int]]]
             [:units [:vector [:map
                               [:name [:maybe :string]]
                               [:description [:maybe :string]]]]]])
  ())

