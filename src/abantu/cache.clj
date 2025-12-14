(ns abantu.cache
  (:require [abantu.util :as util]))

(defprotocol Cache
  (get-item [this uuid])
  (add-item [this item]
    [this item uuid])
  (remove-item [this uuid])
  (get-all-items [this]))

(defn- -get-item [*cache uuid]
  (get-in @*cache [uuid]))

(defn- -remove-item [*cache uuid]
  (swap! *cache dissoc uuid))

(defn- -add-item [*cache item uuid]
  (swap! *cache assoc uuid item)
  {:item item
   :uuid uuid})

(defn -get-all-items [*cache]
  @*cache)

(defn create-cache []
  (let [lcache (atom {})]
    (reify Cache
      (get-item [_ uuid]
        (-get-item lcache uuid))
      (remove-item [_ uuid]
        (-remove-item lcache uuid))
      (add-item [_ item]
        (-add-item lcache item (util/uuid)))
      (add-item [_ item uuid]
        (-add-item lcache item uuid))
      (get-all-items [_]
        (-get-all-items lcache)))))

(comment
  (def cache (create-cache))
  (add-item cache "testitem" "a1b2c3d4e5f6")
  (add-item cache "anothertestitem" "cheese")
  (add-item cache "yetanothertestitem")
  (get-item cache "a1b2c3d4e5f6")
  (remove-item cache "a1b2c3d4e5f6")
  (get-all-items cache))

