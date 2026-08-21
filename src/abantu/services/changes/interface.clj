(ns abantu.services.changes.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]))

(malt/defprotocol VersionControlQuery
  (lookup [input])
  (find [input])
  (all []))

(malt/defprotocol VersionControlMutation)

(defn use-query [])

(defn use-mutation [])
