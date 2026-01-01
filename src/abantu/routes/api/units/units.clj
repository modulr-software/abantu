(ns abantu.routes.api.units.units
  (:require [abantu.services.interface :as services]
            [ring.util.response :as res]
            [clj-fuzzy.levenshtein :as levenshtein]
            [abantu.routes.openapi :as api]
            [abantu.db.interface :as db]))

(defn get-all-units []
  (let [ds  (db/ds :master)
        units (db/find ds
                       {:tname :units
                        :ret :*})
        creator-ids (->> units
                         (mapv :creator-id)
                         (set))
        creators (->> creator-ids
                      (mapv #(db/find ds
                               {:tname :users
                                :where [:= :id %]
                                :ret :1}))
                      (reduce #(assoc %1 (:id %2) %2) {}))]
    (->>
     (mapv #(assoc % :creator (get-in creators [(:creator-id %)])) units)
     (mapv #(dissoc % :creator-id)))))

(get-all-units)

(defn get
  {:summary "get all units"
   :responses (api/success api/GetUnitsResponse)}
  []
  (res/response (services/))
  )

(defn post [])
