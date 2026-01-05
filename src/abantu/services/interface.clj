(ns abantu.services.interface
  (:require [abantu.services.vocab :as vocab]))

(defn vocab-query
  "Generic select query function for returning vocabulary data from the vocab table"
  [ds {:keys [_select _order-by _group-by _limit _type _xhosa _english _noun-class _where _ret] :as opts}]
  (vocab/vocab-query ds opts))

(defn types
  "Returns all unique parts of speech in the vocab database in a hash set."
  [ds]
  (vocab/types ds))

(defn update-vocab! [ds opts]
  (vocab/update-vocab! ds opts))

(defn vocab-by-id
  "Get a specific vocab record by id"
  [ds {:keys [_id] :as opts}]
  (vocab/vocab-by-id ds opts))

(defn vocab
  "Get all vocab records"
  [ds]
  (vocab/vocab ds))

(defn noun-classes [ds]
  (vocab/noun-classes ds))

(defn vocab-by-noun-class [ds {:keys [_noun-class] :as opts}]
  (vocab/vocab-by-noun-class ds opts))

(defn parts-of-speech [ds]
  (vocab/parts-of-speech ds))

(defn vocab-by-part-of-speech [ds {:keys [_type] :as opts}]
  (vocab/vocab-by-part-of-speech ds opts))

(defn delete-vocab! [ds {:keys [_id] :as opts}]
  (vocab/delete-vocab! ds opts))

(defn insert-vocab! [ds {:keys [_data] :as opts}]
  (vocab/insert-vocab! ds opts))
