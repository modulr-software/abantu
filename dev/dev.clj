(ns dev
  (:require [abantu.server :as server]
            [clj-reload.core :as clj-reload]))

(clj-reload/init
  {:dirs ["src" "dev" "test"]})

(defn reload []
  (clj-reload/reload))

(defn before-ns-unload []
  (when (server/running?)
    (server/stop-server)))

(defn after-ns-reload []
  (server/start-server))

(comment
  (reload)
  (server/start-server)
  (server/stop-server)
  (server/running?)
  (server/restart-server)
  (reload))
