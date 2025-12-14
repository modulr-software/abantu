(ns abantu.config
  (:require [aero.core :as aero]
            [malli.core :as m]
            [malli.transform :as mt]
            [malli.error :as me]
            [clojure.java.io :as io]))

(def ^:private oauth2-provider-schema
  [:map
   [:authorization-uri :string]
   [:access-token-uri :string]
   [:redirect-uri :string]
   [:client-id :string]
   [:client-secret :string]
   [:access-query-param :keyword]
   [:scope [:vector {:min 1} :string]]
   [:grant-type :string]])

(def ^:private schema
  [:map
   [:supersecretkey [:string {:min 32}]]
   [:admins-path {:optional true} :string]
   [:admins-encrypted-path :string]
   [:cors-origin :string]
   [:env :string]
   [:database [:map
               [:dir :string]
               [:type :string]]]
   [:oauth2 [:map-of keyword? oauth2-provider-schema]]])

(defn- load-config []
  (let [config (aero/read-config (io/resource "config.edn"))
        decoded (m/decode schema config mt/string-transformer)]
    (when-not (m/validate schema decoded)
      (println (->> decoded
                    (m/explain schema)
                    (me/humanize)))
      (throw (Exception. "Invalid Config")))
    decoded))

(defn read-value
  "Loads in validated config and uses get-in with ks as an argument"
  [& ks]
  (-> (load-config)
      (get-in ks)))

(comment
  (read-value :supersecretkey)
  (read-value :database :dir)
  (read-value :oauth2 :google)
  (read-value :cors-origin)
  (read-value :admins-path)
  (load-config))

