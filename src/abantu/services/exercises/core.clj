(ns abantu.services.exercises.core
  (:require [abantu.db.interface :as db]
            [abantu.services.comments :as comments]
            [honey.sql.helpers :as h]
            [clojure.string :as str]
            [abantu.services.exercises.core :as exercises]))

(defn remove-first [v target]
  (let [[before [_ & after]] (split-with #(not= % target) v)]
    (vec (concat before after))))

(defn- process-options [exercise]
  (update-in exercise
             [:options]
             #(-> (clojure.string/split % #";;")
                  (vec))))

(defn- save-answers-for-exercise! [ds exercise-id answer-type answers]
  (->> (mapv (comp #(merge {:exercise-id exercise-id} %)
                   #(if (= answer-type "bubbles")
                      (assoc % :text (str/join ";;" (:text %)))
                      %))
             answers)
       (assoc {:tname :answers :ret :*} :data)
       (db/insert! ds)))

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

(defn -lookup  [ds {:keys [id unit-id course-id]}]
  (->> (db/find ds (cond-> {:tname :exercises
                            :ret :1}
                     (some? id) (h/where [:= :id id])
                     (some? unit-id) (h/where [:= :unit-id unit-id])
                     (some? course-id) (h/where [:= :course-id course-id])))
       (add-comments ds)
       (process-options)
       (add-answers ds)))

(defn -all [ds]
  (->> (db/find ds {:tname :exercises
                    :ret :*})
       (mapv (comp (partial add-comments ds)
                   process-options
                   (partial add-answers ds)))))

(defn -find [ds {:keys [id unit-id course-id]}]
  (->> (db/find ds (cond-> {:tname :exercises
                            :ret :*}
                     (some? id) (h/where [:= :id id])
                     (some? unit-id) (h/where [:= :unit-id unit-id])
                     (some? course-id) (h/where [:= :course-id course-id])))
       (mapv (comp (partial add-comments ds)
                   process-options
                   (partial add-answers ds)))))

(defn- exists? [ds id]
  (when-let [exercise (-lookup ds {:id id})]
    exercise))

(defmulti apply-update (fn [_ action] (:type action)))

(defmethod apply-update :set-unit [exercise {:keys [payload]}]
  (assoc exercise :unit-id (:unit-id payload)))

(defmethod apply-update :set-instruction [exercise {:keys [payload]}]
  (assoc exercise :instruction (:instruction payload)))

(defmethod apply-update :set-question-content [exercise {:keys [payload]}]
  (assoc exercise :question-content (:question-content payload)))

(defmethod apply-update :set-answer-type [exercise {:keys [payload]}]
  (assoc exercise :answer-type (:answer-type payload)))

(defmethod apply-update :set-level [exercise {:keys [payload]}]
  (assoc exercise :level (:level payload)))

(defmethod apply-update :set-correct-message [exercise {:keys [payload]}]
  (assoc exercise :correct-message (:correct-message payload)))

(defmethod apply-update :set-incorrect-message [exercise {:keys [payload]}]
  (assoc exercise :incorrect-message (:incorrect-message payload)))

(defmethod apply-update :set-position [exercise {:keys [payload]}]
  (assoc exercise :position (:position payload)))

(defmethod apply-update :set-options [exercise {:keys [payload]}]
  (assoc exercise :options (:options payload)))

(defmethod apply-update :add-option [exercise {:keys [payload]}]
  (assoc exercise :options (conj (:options exercise) (:option payload))))

(defmethod apply-update :remove-option [exercise {:keys [payload]}]
  (assoc exercise :options (remove-first (:options exercise) (:option payload))))

(defmethod apply-update :set-answers [exercise {:keys [payload]}]
  (->> (:answers payload)
       (mapv #(assoc % :exercise-id (:id exercise)))
       (assoc exercise :answers)))

(defmethod apply-update :add-answer [exercise {:keys [payload]}]
  (->> (:id exercise)
       (assoc (:answer payload) :exercise-id)
       (conj (:answers exercise))
       (assoc exercise :answers)))

(defmethod apply-update :remove-answer [{:keys [answers] :as exercise} {:keys [payload]}]
  (assoc exercise :answers (vec (remove #(= (:id %) (:answer-id payload)) answers))))

(defn -set-unit [ds {:keys [id unit-id] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:unit-id unit-id}})
    (apply-update exercise {:type :set-unit
                            :payload update})))

(defn -set-instruction [ds {:keys [id instruction] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:instruction instruction}})
    (apply-update exercise {:type :set-instruction
                            :payload update})))

(defn -set-question-content [ds {:keys [id question-content] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:question-content question-content}})
    (apply-update exercise {:type :set-question-content
                            :payload update})))

(defn -set-answer-type [ds {:keys [id answer-type] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:answer-type answer-type}})
    (apply-update exercise {:type :set-answer-type
                            :payload update})))

(defn -set-level [ds {:keys [id level] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:level level}})
    (apply-update exercise {:type :set-level
                            :payload update})))

(defn -set-correct-message [ds {:keys [id correct-message] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:correct-message correct-message}})
    (apply-update exercise {:type :set-correct-message
                            :payload update})))

(defn -set-incorrect-message [ds {:keys [id incorrect-message] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:incorrect-message incorrect-message}})
    (apply-update exercise {:type :set-incorrect-message
                            :payload update})))

(defn -set-position [ds {:keys [id position] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:position position}})
    (apply-update exercise {:type :set-position
                            :payload update})))

(defn -set-options [ds {:keys [id options] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" options)}})
    (apply-update exercise {:type :set-options
                            :payload update})))

(defn -add-option [ds {:keys [id option] :as update}]
  (when-let [{:keys [options] :as exercise} (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" (conj options option))}})
    (apply-update exercise {:type :add-option
                            :payload update})))

(defn -remove-option [ds {:keys [id option] :as update}]
  (when-let [{:keys [options] :as exercise} (exists? ds id)]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" (remove-first options option))}})
    (apply-update exercise {:type :remove-option
                            :payload update})))

(defn -set-answers [ds {:keys [id answers] :as update}]
  (when-let [{:keys [answer-type] :as exercise} (exists? ds id)]
    (save-answers-for-exercise! ds id answer-type answers)
    (apply-update exercise {:type :set-answers
                            :payload update})))

(defn -add-answer [ds {:keys [id answer] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/insert! ds {:tname :answers
                    :values (->> (:text answer)
                                 (str/join ";;")
                                 (assoc answer :exercise-id id :text))})
    (apply-update exercise {:type :add-answer
                            :payload update})))

(defn -remove-answer [ds {:keys [id answer-id] :as update}]
  (when-let [exercise (exists? ds id)]
    (db/delete! ds {:tname :answers
                    :where [:= :id answer-id]})
    (apply-update exercise {:type :remove-answer
                            :payload update})))

(comment
  (def ds (db/ds :master))
  (-lookup (db/ds :master) {:id 1})
  (-find (db/ds :master) {:id 1})
  (-all (db/ds :master))

  (db/insert! ds {:tname :courses
                  :values {:name "afrikaans course"
                           :language "afrikaans"
                           :description "learn about pronouns"}})

  (db/insert! ds {:tname :units
                  :values {:name "pronouns"
                           :description "learn about pronouns"
                           :level 1
                           :type "lesson"
                           :course-id 1}})
  (db/insert! ds {:tname :units
                  :values {:name "pronouns II"
                           :description "learn more about pronouns"
                           :level 2
                           :type "lesson"
                           :course-id 1}})

  (db/insert! ds {:tname :exercises
                  :values {:unit-id 1
                           :course-id 1
                           :instruction "translate the following"
                           :question-content "I"
                           :answer-type "bubbles"
                           :options "ek;;jy;;hy;;sy"
                           :correct-message "correct!"
                           :incorrect-message "o nei"}})
  (db/insert! ds {:tname :answers
                  :values {:exercise-id 1
                           :text "ek"}})

  (-set-unit ds {:id 1
                 :unit-id 2})
  (-set-instruction ds {:id 1
                        :instruction "Translate the following:"})
  (-set-question-content ds {:id 1
                             :question-content "I am"})
  (-set-answer-type ds {:id 1
                        :answer-type "bubbles"})
  (-set-level ds {:id 1
                  :level 2})
  (-set-correct-message ds {:id 1
                            :correct-message "wow amazing!"})
  (-set-incorrect-message ds {:id 1
                              :incorrect-message "bro that was so lame"})
  (-set-position ds {:id 1
                     :position 1})
  (-set-options ds {:id 1
                    :options ["ek" "hy" "sy" "is"]})
  (-add-option ds {:id 1
                   :option "weet"})
  (-remove-option ds {:id 1
                      :option "weet"})
  (-set-answers ds {:id 1
                    :answers [{:text ["ek" "is"]}]})
  (-add-answer ds {:id 1
                   :answer {:text ["hy" "is"]}})
  (-remove-answer ds {:id 1 
                      :answer-id 3})

  :end)
