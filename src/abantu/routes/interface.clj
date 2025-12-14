(ns abantu.routes.interface
  (:require [abantu.routes.reitit :as reitit]))

(defn create-app [{:keys [_ds] :as opts}]
  (reitit/create-app opts))