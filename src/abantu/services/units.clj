(ns abantu.services.units
  (:require [abantu.db.interface :as db]
            [clojure.string :as str]
            [abantu.db.util :as db.util]
            [abantu.db.honey :as hon]
            [abantu.services.comments :as comments]))

(defn- process-options [exercise]
  (update-in exercise
             [:options]
             #(-> (clojure.string/split % #";;")
                  (vec))))

(defn- encode-options [{:keys [options] :as exercise}]
  (update-in exercise
             [:options]
             #(str/join ";;" %)))

(defn- add-answers [ds {:keys [id answer-type] :as exercise}]
  (let [answers (db/find ds {:tname :answers
                             :where [:= :exercise-id id]
                             :ret :*})
        answers (if (= answer-type "bubbles")
                  (mapv #(assoc % :text (str/split (:text %) #";;")) answers)
                  answers)]
    (assoc exercise :answers answers)))

(defn- add-comments [ds {:keys [id] :as exercise}]
  (assoc exercise :comments (comments/get-for-exercise ds id)))

(defn get-exercises-for-unit
  "Get all exercises with answers for a given unit-id"
  [ds id]
  (->> (db/find ds {:tname :exercises
                    :where [:= :unit-id id]
                    :order-by [[:position :asc]]
                    :ret :*})
       (mapv (comp (partial add-comments ds)
                   process-options
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
                    :order-by [[:position :asc]]
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

(defn save-exercise! [ds {:keys [options answers answer-type unit-id] :as exercise}]
  (let [existing-exercises (db/find ds {:tname :exercises
                                        :where [:= :unit-id unit-id]
                                        :ret :*})
        {:keys [id] :as result}
        (db/insert! ds {:tname :exercises
                        :data (-> (dissoc exercise :answers)
                                  (assoc :options (str/join ";;" options)
                                         :position (inc (count existing-exercises))))
                        :ret :1})]
    (when (seq answers)
      (save-answers-for-exercise! ds id answer-type answers))
    (assoc result
           :answers (get-answers-for-exercise ds id)
           :options options)))

(defn save-exercises! [ds exercises]
  (mapv (partial save-exercise! ds)
        exercises))

(defn save-unit! [ds {:keys [exercises course-id] :as unit}]
  (let [existing-units (db/find ds {:tname :units
                                    :where [:= :course-id course-id]
                                    :ret :*})
        {:keys [id] :as result} (db/insert! ds {:tname :units
                                                :data (-> (dissoc unit :exercises)
                                                          (assoc :position (inc (count existing-units)))) 
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
  (->> (db/find ds {:tname :exercises
                    :where [:= :id id]
                    :ret :1})
       (add-comments ds)
       (add-answers ds)
       (process-options)))

(defn- encode-answer [answer]
  (update-in answer [:text] #(str/join ";;" %)))

(defn update-exercise! [ds {:keys [answers id] :as exercise}]
  (when (contains? exercise :answers)
    (db/delete! ds {:tname :answers
                    :where [:= :exercise-id id]
                    :ret :*})
    (when (seq answers)
      (db/insert! ds {:tname :answers
                      :data (->> answers
                                 (mapv #(assoc % :exercise-id id))
                                 (mapv encode-answer))
                      :ret :*})))
  (let [data (if (contains? exercise :options)
               (encode-options (dissoc exercise :answers))
               (dissoc exercise :answers :options))]
    (db/update! ds {:tname :exercises
                    :data data
                    :where [:= :id id]
                    :ret :1})))

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

(defn change-exercise-order [ds exercise-id position]
  (let [exercise (db/find-one ds {:tname :exercises
                                  :where [:= :id exercise-id]
                                  :ret :1})]
    (when (nil? exercise)
      (throw (ex-info (str "Unable to find exercise with id " exercise-id " and set its position to " position)
                      {:panic? "Yes, some user's may not be able to change the order of exercises within a unit."
                       :next-steps "Debug this by comparing the input from the admin portal to the database state."})))
    (db/update! ds {:tname :exercises
                    :where [:= :id exercise-id]
                    :data {:position position}})
    (some? exercise)))

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

  (db/find ds {:tname :answers
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
