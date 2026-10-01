(ns abantu.services.comments.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.services.comments.core :as comments]
            [abantu.util :as util]
            [abantu.db.util :as db.util]))

(def ?Comment [:map
               [:exercise-id :int]
               [:unit-id :int]
               [:course-id :int]
               [:text :string]
               [:timestamp :string]
               [:resolved :int]
               [:resolved-by (util/maybe :int)]
               [:resolved-at (util/maybe :string)]])

(def ?Lookup
  [:map [:id :int]])

(def ?Find
  [:map
   [:id (util/maybe :int)]
   [:course-id (util/maybe :int)]
   [:unit-id (util/maybe :int)]
   [:exercise-id (util/maybe :int)]
   [:resolved (util/maybe :boolean)]])

(malt/defprotocol CommentQuery
  (lookup [input ?Lookup] (util/maybe ?Comment))
  (find [input ?Find] [:vector ?Comment])
  (all [] [:vector ?Comment]))

(defn use-query
  ([] (use-query (db.util/conn)))
  ([ds]
   (malt/reify CommentQuery
     (lookup [_ input]
       (comments/-lookup ds input))
     (find [_ input]
       (comments/-find ds input))
     (all [_]
       (comments/-all ds)))))

(comment

  (def cq (use-query (db.util/conn)))
  (all cq)

  :end)
