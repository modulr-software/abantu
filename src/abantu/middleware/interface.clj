(ns abantu.middleware.interface
  (:require [abantu.middleware.core :as mw]))

(defn apply-generic [app & {:keys [ds store js]}]
  (mw/apply-generic app :ds ds :store store :js js))

