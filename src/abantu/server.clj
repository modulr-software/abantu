(ns abantu.server
  (:require [org.httpkit.server :as http]
            [abantu.db.interface :as db]
            [abantu.routes.interface :as routes]
            [abantu.util :as util]))

(defonce ^:private *components (atom nil))

(defn initialise-server! [{:keys [ds]}]
  (http/run-server
   (routes/create-app {:ds ds})
   {:port 3000}))


(defn component-on? [component]
  (if (some? (get @*components component))
    true
    false))

(defn deps-on? [deps]
  (every? component-on? deps))

(defn initialise!
  "executes the init-fn on the provided component and, if successful, updates the components atom with the new component"
  [{:keys [name init-fn deps] :as _component}]
  (try
    (when (deps-on? deps)
      (swap! *components assoc name (init-fn @*components)))
    (catch Exception e (println (str "Failed to initialise " name ":") e))))

(defn initialise-components! [components]
  (run! initialise! components))

(defn running? []
  (some? (:server @*components)))

(defn start-server []
  (cond (not (some? (:server @*components)))
        (do
          (println "Starting server on port 3000...")
          (initialise-components! [{:name :ds
                                    :init-fn (fn [_deps] (db/ds :master))
                                    :deps []}
                                   {:name :server
                                    :deps [:ds]
                                    :init-fn initialise-server!}]))
        :else
        (println "Server already running!")))

(defn stop-server []
  (println "Stopping server...")
  (when (some? (:server @*components))
    (let [server-stop (:server @*components)]
      (server-stop)))
  (reset! *components nil))

(defn restart-server [& {:keys [keep-js]}]
  (if keep-js
    (do
      (when (some? (:server @*components))
        (let [server-stop (:server @*components)]
          (server-stop)))
      (swap! *components select-keys [:js])
      (initialise-components! [{:name :ds
                                :init-fn (fn [_deps] (db/ds :master))
                                :deps []}
                               {:name :server
                                :deps [:ds]
                                :init-fn initialise-server!}]))
    (do
      (stop-server)
      (start-server))))

(comment
  (start-server)
  (stop-server)
  (restart-server)
  (restart-server :keep-js true)
  (def test-wrapper
    (util/wrap-json (fn [request] request)))
  (test-wrapper {:status 200
                 :body "{\"value\":\"Hello, Source!\"}"
                 :headers {"Content-Type" "application/json"}})
  ())
