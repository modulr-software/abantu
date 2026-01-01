(ns abantu.services.units
  (:require [abantu.db.interface :as db]))


(defn- process-options [exercise]
  (update-in exercise
             [:options]
             #(-> (clojure.string/split % #",")
                  (vec))))

(defn- add-answers [ds {:keys [id] :as exercise}]
  (assoc exercise :answers (db/find ds {:tname :answers
                                        :where [:= :exercise-id id]
                                        :ret :*})))

(defn get-exercises-for-unit [ds id]
    (->> (db/find ds {:tname :exercises
                      :where [:= :unit-id id]
                      :ret :*})
         (mapv (comp process-options
                     (partial add-answers ds)))))

(defn get-unit [ds id]
  (let [unit (db/find ds {:tname :units
                          :where [:= :id id]
                          :ret :1})
        exercises (get-exercises-for-unit ds id)]
    (cond-> unit
      (seq exercises) (assoc :exercises exercises))))

(defn- add-exercises-to-unit [ds {:keys [id] :as unit}]
  (assoc unit :exercises (get-exercises-for-unit ds id)))
(defn- add-creator-to-unit [ds {:keys [creator-id] :as unit}]
  (-> (dissoc unit :creator-id)
      (assoc :creator (db/find ds {:tname :users
                                   :where [:= :id creator-id]
                                   :ret :1}))))
(defn get-all-units [ds]
  (let [units (db/find ds {:tname :units
                           :ret :*})]
    (mapv (comp (partial add-exercises-to-unit ds)
                (partial add-creator-to-unit ds)) units))
  )

(defn save-exercise! [ds {:keys [answers] :as exercise}]
  (let [{:keys [id] :as result} (db/insert! ds {:tname :exercises
                               :data (dissoc exercise :answers)
                               :ret :1})
        answers (mapv #(assoc % :exercise-id id) answers)
        inserted-answers (when (seq answers)
                           (db/insert! ds {:tname :answers
                                           :data answers
                                           :ret :*}))]
    (assoc result :answers inserted-answers)))

(defn save-exercises! [ds exercises]
  (run! (partial save-exercise! ds)
        exercises))

;; TODO update the thing to store exercises on a unit as well
(defn save-unit! [ds {:keys [exercises] :as unit}]
  (let [{:keys [id] :as result} (db/insert! ds {:tname :units
                               :data (dissoc unit :exercises)
                               :ret :1})]
    (when (seq exercises)
      (save-exercises! ds (mapv #(assoc % :unit-id id) exercises)))
    result))

(defn save-units! [ds units]
  (run! (partial save-unit! ds) units))

(comment
  
  (def ds (db/ds :master))
  (save-units! ds [{:name "some unit 2"
                    :description "some unit type shit 2"
                    :creator-id 1
                    :exercises [{:question-type "translation"
                                 :question "imoto"
                                 :options "a,the,car,cars,motorbike"
                                 :answers [{:text "a,car"} {:text "the,car"}]}]}])
  
  (get-all-units ds)

  (db/find ds {:tname :answers
               :where [:= :exercise-id nil]
               :ret :*})
  (db/delete! ds {:tname :units
                 :where [:= :id 2]
                 :ret :*})

  (db/delete! ds {:tname :units
                 :where [:= :creator-id nil] 
                 :ret :*})

  ()
  )
