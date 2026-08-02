(ns abantu.config
  (:require [aero.core :as aero]
            [malli.core :as m]
            [malli.transform :as mt]
            [malli.error :as me]
            [clojure.java.io :as io]))

(def ^:private schema
  [:map
   [:supersecretkey [:string {:min 32}]]
   [:admins-path {:optional true} :string]
   [:admins-encrypted-path {:optional true} :string]
   [:cors-origin :string]
   [:env :string]
   [:port :int]
   [:base-url :string]
   [:admin-portal-url :string]
   [:app-url :string]
   [:database [:map
               [:dir :string]
               [:type :string]]]
   [:openai :string]])

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

(defn read-cors-with-port []
  #_(str (read-value :cors-origin) ":" (read-value :port))
  #"^https?:\/\/localhost(?::\d+)?$"
  )

(defn read-allowed-cors-origins []
  [(read-cors-with-port)
   #"^(?:https:\/\/)?abantumobile\.expo\.app\/?$"])

(defn read-path-prefix []
  (or (read-value :path-prefix) ""))

(defn read-api-url
  "API origin for the current env. Prod reads :base-url; non-prod builds
  http://localhost:<port> from :port. `read-cors-with-port` is a CORS regex
  matcher; this is its string counterpart."
  []
  (case (read-value :env)
    "prod" (read-value :base-url)
    (str "http://localhost:" (read-value :port))))

(defn read-admin-portal-url
  "Admin portal origin (dashboard) for email links. Read straight from config;
  set ADMIN_PORTAL_URL in .env to override."
  []
  (read-value :admin-portal-url))

(defn read-app-url
  "App origin (learner app) for email links. Read straight from config;
  set APP_URL in .env to override."
  []
  (read-value :app-url))

(comment
  (read-value :supersecretkey)
  (read-value :database :dir)
  (read-value :oauth2 :google)
  (read-value :cors-origin)
  (read-value :admins-path)
  (read-value :port)
  (load-config)
  (read-cors-with-port)
  (read-api-url)
  (read-admin-portal-url)
  (read-app-url))

