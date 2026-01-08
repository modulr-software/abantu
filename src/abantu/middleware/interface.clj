(ns abantu.middleware.interface
  (:require [abantu.middleware.core :as mw]))

(defn apply-validation [app openapi-meta]
  (mw/wrap-input-validation app openapi-meta))

(defn apply-generic [app & {:keys [ds store js]}]
  (mw/apply-generic app :ds ds :store store :js js))

