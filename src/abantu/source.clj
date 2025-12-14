(ns abantu.source
  (:require [abantu.server :as server]
            [abantu.hooks :as hooks])
  (:gen-class))

(defn -main [& _]
  (server/start-server)
  (hooks/add-shutdown-hook server/stop-server))
