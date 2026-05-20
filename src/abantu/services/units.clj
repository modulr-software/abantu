(ns abantu.services.units
  (:require [abantu.db.interface :as db]
            [clojure.string :as str]
            [abantu.db.util :as db.util]
            [abantu.db.honey :as hon]))

(defn- process-options [exercise]
  (update-in exercise
             [:options]
             #(-> (clojure.string/split % #";;")
                  (vec))))

(defn- add-answers [ds {:keys [id answer-type] :as exercise}]
  (let [answers (db/find ds {:tname :answers
                             :where [:= :exercise-id id]
                             :ret :*})
        answers (if (= answer-type "bubbles")
                  (mapv #(assoc % :text (str/split (:text %) #";;")) answers)
                  answers)]
    (assoc exercise :answers answers)))

(defn get-exercises-for-unit
  "Get all exercises with answers for a given unit-id"
  [ds id]
  (->> (db/find ds {:tname :exercises
                    :where [:= :unit-id id]
                    :ret :*})
       (mapv (comp process-options
                   (partial add-answers ds)))))

(defn- add-exercises-to-unit [ds {:keys [id] :as unit}]
  (let [exercises (get-exercises-for-unit ds id)]
    (if (seq exercises)
      (assoc unit :exercises exercises)
      unit)))

(defn get-unit
  "get a unit with all exercises for a given unit-id"
  [ds id]
  (let [unit (db/find ds {:tname :units
                          :where [:= :id id]
                          :ret :1})]
    (add-exercises-to-unit ds unit)))

(defn get-units
  "Get all units with exercises for a given course-id"
  [ds course-id]
  (->> (db/find ds {:tname :units
                    :where [:= :course-id course-id]
                    :ret :*})
       (mapv #(assoc % :exercises
                     (get-exercises-for-unit ds (:id %))))))

(defn- add-exercises-to-unit [ds {:keys [id] :as unit}]
  (assoc unit :exercises (get-exercises-for-unit ds id)))

(defn get-all-units [ds]
  (let [units (db/find ds {:tname :units
                           :ret :*})]
    (mapv (partial add-exercises-to-unit ds) units)))

(defn get-answer-type [ds exercise-id]
  (->>
   (db/find ds {:tname :exercises
                :where [:= :id exercise-id]
                :ret :1})
   (:answer-type)))

(defn get-answers-for-exercise [ds exercise-id]
  (let [answer-type (get-answer-type ds exercise-id)
        answers (db/find ds {:tname :answers
                             :where [:= :exercise-id exercise-id]
                             :ret :*})]
    (->> answers
         (mapv #(if (= answer-type "bubbles")
                  (assoc % :text (str/split (:text %) #";;"))
                  %)))))

(defn save-answers-for-exercise! [ds exercise-id answer-type answers]
  (->> (mapv (comp #(merge {:exercise-id exercise-id} %)
                   #(if (= answer-type "bubbles")
                      (assoc % :text (str/join ";;" (:text %)))
                      %))
             answers)
       (assoc {:tname :answers :ret :*} :data)
       (db/insert! ds)))

(defn save-exercise! [ds {:keys [options answers answer-type] :as exercise}]
  (let [{:keys [id] :as result}
        (db/insert! ds {:tname :exercises
                        :data (-> (dissoc exercise :answers)
                                  (assoc :options (str/join ";;" options)))
                        :ret :1})]
    (when (seq answers)
      (save-answers-for-exercise! ds id answer-type answers))
    (assoc result
           :answers (get-answers-for-exercise ds id)
           :options options)))

(defn save-exercises! [ds exercises]
  (mapv (partial save-exercise! ds)
        exercises))

(defn save-unit! [ds {:keys [exercises] :as unit}]
  (let [{:keys [id] :as result} (db/insert! ds {:tname :units
                                                :data (dissoc unit :exercises)
                                                :ret :1})]
    (->> (when (seq exercises)
           (save-exercises! ds (mapv #(assoc % :unit-id id) exercises)))
         (assoc result :exercises))))

(defn save-units! [ds units]
  (mapv (partial save-unit! ds) units))

(defn update-unit! [ds {:keys [id] :as unit}]
  (db/update! ds {:tname :units
                  :where [:= :id id]
                  :data (dissoc unit :id)})
  (db/find-one ds {:tname :units
                   :where [:= :id id]
                   :ret :1}))

(defn get-exercise [ds id]
  (add-answers
   ds (db/find
       ds {:tname :exercises
           :where [:= :id id]
           :ret :1})))

(defn update-exercise! [ds {:keys [answers id] :as exercise}]
  (when (seq answers)
    (db/delete! ds {:tname :answers
                    :where [:= :exercise-id id]
                    :ret :*})
    (db/insert! ds {:tname :answers
                    :data (mapv #(assoc % :exercise-id id) answers)
                    :ret :*}))
  (db/update! ds {:tname :exercises
                  :data (dissoc exercise :answers)
                  :where [:= :id id]}))

(defn delete-exercise [ds id]
  (let [{:keys [id answers] :as exercise} (get-exercise ds id)]
    (when (seq answers)
      (db/delete! ds {:tname :answers
                      :where [:= :exercise-id id]
                      :ret :1}))
    (when (some? exercise)
      (db/delete! ds {:tname :exercises
                      :where [:= :id id]
                      :ret :1}))))

(defn delete-unit! [ds id]
  (let [unit (get-unit ds id)
        exercises (:exercises unit)]
    (when (seq exercises)
      (run! (partial delete-exercise ds) (mapv :id exercises)))
    (when (some? unit)
      (db/delete! ds {:tname :units
                      :where [:= :id id]
                      :ret :1}))
    (some? unit)))

(comment
  (def ds (db/ds :master))
  (save-units! ds [{:name "some unit 1"
                    :description "some unit type shit"
                    :creator-id 1
                    :exercises [{:question-type "translation"
                                 :question "isiXhosa"
                                 :options "Xhosa,Xhosas,a,It's"
                                 :answers [{:text "Xhosa"}]}]}])

  (db/find ds {:tname :exercises
               :ret :*})

  (get-exercises-for-unit ds 1)

  (def test-insert (->> (get-exercises-for-unit ds 1)
                        (first)))
  (save-exercise! ds (-> (dissoc test-insert :id)
                         (assoc :question "is this a test question?"
                                :question-type "multiple-choice"
                                :options ["yes" "no"]
                                :answers ["yes"]
                                :level 1)))
  (save-exercise! ds (-> (dissoc test-insert :id)
                         (assoc :question "what is the question?"
                                :question-type "translation"
                                :options ["umbuzo" "imibuzo" "yintoni" "ntoni" "untoni"]
                                :answers [["yintoni" "umbuzo"] ["untoni" "umbuzo"]]
                                :level 1)))

  (get-all-units ds)

  (db/find ds {:tname :answers
               :where [:= :exercise-id nil]
               :ret :*})
  (db/delete! ds {:tname :units
                  :where [:= :id 1]
                  :ret :*})

  (db/delete! ds {:tname :units
                  :where [:= :creator-id nil]
                  :ret :*})

  ())
