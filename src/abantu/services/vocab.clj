(ns abantu.services.vocab
  (:require [abantu.db.interface :as db]))


(defn types
  "Returns all unique parts of speech in the vocab database in a hash set."
  [ds]
  (->>
   (db/find ds {:tname :vocab
                :ret :*})
   (mapv :type)
   set))


(defn update! [ds opts]
  (update! ds (merge {:tname :vocab} opts)))

(defn vocab-by-id
  "Get a specific vocab record by id"
  [ds {:keys [id] :as _opts}]
  (db/find ds {:tname :vocab
            :where [:= :id id]
            :ret :1}))

(defn vocab
  "Get all vocab records"
  [ds]
  (db/find ds {:tname :vocab
               :ret :*}))

(defn noun-classes [ds]
  (->>
   (db/find ds {:tname :vocab
                :ret :*})
   (mapv :noun-class)
   (filterv identity)
   (set)))

(defn vocab-by-noun-class [ds {:keys [noun-class] :as _opts}]
  (db/find ds {:tname :vocab
               :where [:= :noun-class noun-class]
               :ret :*}))

(defn parts-of-speech [ds]
  (->>
   (db/find ds {:tname :vocab
                :ret :*})
   (mapv :type)
   (filterv identity)
   (set)))

(defn vocab-by-part-of-speech [ds {:keys [type] :as _opts}]
  (db/find ds {:tname :vocab
               :where [:= :type type]
               :ret :*}))

(defn delete-vocab! [ds {:keys [id] :as _opts}]
  (db/delete! ds {:tname :vocab
                  :where [:= :id id]
                  :ret :*}))

(defn insert-vocab! [ds {:keys [values] :as _opts}]
  (db/insert! ds {:tname :vocab
                  :values values
                  :ret :*}))


(comment
  (def ds (db/ds :master))
  (types ds)
  (->>
   (vocab ds)
   (count)
   (assoc {} :vocab-count))
  (vocab-by-id ds {:id 2955})
  (noun-classes ds)
  (vocab-by-noun-class ds {:noun-class (-> (noun-classes ds)
                                           (first))})
  (parts-of-speech ds)
  (->
   (vocab-by-part-of-speech ds {:type "adjective"})
   (count))
  ()
  )