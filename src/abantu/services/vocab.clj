(ns abantu.services.vocab
  (:require [abantu.db.honey :as hon]
            [honey.sql.helpers :as hsql]))

(defn vocab-query
  "Generic select query function for returning vocabulary data from the vocab table"
  [ds {:keys [select order-by group-by limit type xhosa english noun-class where ret]}]
  (let [clauses (cond-> {}
                  (some? where) (merge where)
                  (some? type) (hsql/where [:= :type type])
                  (some? xhosa) (hsql/where [:like :xhosa xhosa])
                  (some? english) (hsql/where [:like :english english])
                  (some? noun-class) (hsql/where [:like :noun-class noun-class])
                  (some? select) (merge select)
                  (nil? select) (merge {:select :*})
                  (some? limit) (hsql/limit limit)
                  (some? order-by) (merge order-by)
                  (some? group-by) (merge group-by))]
    (hon/execute!
     ds
     (merge {:from [:vocab]}
            clauses)
     {:ret (if ret ret :*)})))

(defn types
  "Returns all unique parts of speech in the vocab database in a hash set."
  [ds]
  (->>
   (hon/find ds {:tname :vocab
                 :ret :*})
   (mapv :type)
   set))

(defn update-vocab! [ds opts]
  (hon/update! ds (merge {:tname :vocab} opts)))

(defn vocab-by-id
  "Get a specific vocab record by id"
  [ds {:keys [id] :as _opts}]
  (hon/find ds {:tname :vocab
                :where [:= :id id]
                :ret :1}))

(defn vocab
  "Get all vocab records"
  [ds]
  (hon/find ds {:tname :vocab
                :ret :*}))

(defn noun-classes [ds]
  (->>
   (hon/find ds {:tname :vocab
                 :ret :*})
   (mapv :noun-class)
   (filterv identity)
   (set)))

(defn vocab-by-noun-class [ds {:keys [noun-class] :as _opts}]
  (hon/find ds {:tname :vocab
                :where [:= :noun-class noun-class]
                :ret :*}))

(defn parts-of-speech [ds]
  (->>
   (hon/find ds {:tname :vocab
                 :ret :*})
   (mapv :type)
   (filterv identity)
   (set)))

(defn vocab-by-part-of-speech [ds {:keys [type] :as _opts}]
  (hon/find ds {:tname :vocab
                :where [:= :type type]
                :ret :*}))

(defn delete-vocab! [ds {:keys [id] :as _opts}]
  (hon/delete! ds {:tname :vocab
                   :where [:= :id id]
                   :ret :*}))

(defn insert-vocab! [ds {:keys [data] :as _opts}]
  (hon/insert! ds {:tname :vocab
                  :data data
                  :ret :1}))

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
  ())
