(ns abantu.yolo.interface
  (:require [abantu.yolo.core :as yolo]))

(defn create-handler [opts]
  (yolo/create-handler opts))