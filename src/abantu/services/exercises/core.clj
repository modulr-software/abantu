(ns abantu.services.exercises.core
  (:require [abantu.db.interface :as db]
            [abantu.services.comments :as comments]
            [abantu.services.exercises.update :as update]
            [honey.sql.helpers :as h]
            [clojure.string :as str]))

(defn- process-options [exercise]
  (update-in exercise
             [:options]
             #(-> (clojure.string/split % #";;")
                  (vec))))

(defn- save-answers-for-exercise! [ds exercise-id answer-type answers]
  (db/delete! ds {:tname :answers
                  :where [:= :exercise-id exercise-id]})
  (->> answers
       (mapv #(if (= answer-type "bubbles")
                (assoc {} :text (str/join ";;" %))
                %))
       (mapv #(assoc % :exercise-id exercise-id))
       (assoc {:tname :answers :ret :*} :data)
       (db/insert! ds)))

(defn- attach-answers [ds {:keys [id answer-type] :as exercise}]
  (let [answers (db/find ds {:tname :answers
                             :where [:= :exercise-id id]
                             :ret :*})
        answers (if (= answer-type "bubbles")
                  (mapv #(assoc % :text (str/split (:text %) #";;")) answers)
                  answers)]
    (assoc exercise :answers answers)))

(defn- attach-comments [ds {:keys [id] :as exercise}]
  (assoc exercise :comments (comments/get-for-exercise ds id)))

(defn -lookup  [ds {:keys [id unit-id course-id]}]
  (->> (db/find ds (cond-> {:tname :exercises
                            :ret :1}
                     (some? id) (h/where [:= :id id])
                     (some? unit-id) (h/where [:= :unit-id unit-id])
                     (some? course-id) (h/where [:= :course-id course-id])))
       (attach-comments ds)
       (process-options)
       (attach-answers ds)))

(defn -all [ds]
  (->> (db/find ds {:tname :exercises
                    :ret :*})
       (mapv (comp (partial attach-comments ds)
                   process-options
                   (partial attach-answers ds)))))

(defn -find [ds {:keys [id unit-id course-id]}]
  (->> (db/find ds (cond-> {:tname :exercises
                            :ret :*}
                     (some? id) (h/where [:= :id id])
                     (some? unit-id) (h/where [:= :unit-id unit-id])
                     (some? course-id) (h/where [:= :course-id course-id])))
       (mapv (comp (partial attach-comments ds)
                   process-options
                   (partial attach-answers ds)))))

(defn -create
  [ds {:keys [options answers] :as update}]
  (let [update' (assoc update :options (str/join ";;" options))
        {:keys [id]} (db/insert! ds {:tname :exercises
                                     :values (dissoc update' :answers)
                                     :ret :1})
        answers' (->> (mapv #(str/join ";;" %) answers)
                      (mapv #(assoc {} :exercise-id id :text %)))]
    (db/insert! ds {:tname :answers
                    :values answers'})
    (update/apply (-lookup ds {:id id}) {:type :create
                                         :payload update})))

(defn -set-unit [ds {:keys [id unit-id] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:unit-id unit-id}})
    (update/apply exercise {:type :set-unit
                            :payload update})))

(defn -set-instruction [ds {:keys [id instruction] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:instruction instruction}})
    (update/apply exercise {:type :set-instruction
                            :payload update})))

(defn -set-question-content [ds {:keys [id question-content] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:question-content question-content}})
    (update/apply exercise {:type :set-question-content
                            :payload update})))

(defn -set-answer-type [ds {:keys [id answer-type] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:answer-type answer-type}})
    (update/apply exercise {:type :set-answer-type
                            :payload update})))

(defn -set-level [ds {:keys [id level] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:level level}})
    (update/apply exercise {:type :set-level
                            :payload update})))

(defn -set-correct-message [ds {:keys [id correct-message] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:correct-message correct-message}})
    (update/apply exercise {:type :set-correct-message
                            :payload update})))

(defn -set-incorrect-message [ds {:keys [id incorrect-message] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:incorrect-message incorrect-message}})
    (update/apply exercise {:type :set-incorrect-message
                            :payload update})))

(defn -set-position [ds {:keys [id position] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:position position}})
    (update/apply exercise {:type :set-position
                            :payload update})))

(defn -set-options [ds {:keys [id options] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" options)}})
    (update/apply exercise {:type :set-options
                            :payload update})))

(defn -add-option [ds {:keys [id option] :as update}]
  (when-let [{:keys [options] :as exercise} (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" (conj options option))}})
    (update/apply exercise {:type :add-option
                            :payload update})))

(defn -remove-option [ds {:keys [id option] :as update}]
  (when-let [{:keys [options] :as exercise} (-lookup ds {:id id})]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" (update/remove-first options option))}})
    (update/apply exercise {:type :remove-option
                            :payload update})))

(defn -set-answers [ds {:keys [id answers] :as update}]
  (when-let [{:keys [answer-type] :as exercise} (-lookup ds {:id id})]
    (save-answers-for-exercise! ds id answer-type answers)
    (update/apply exercise {:type :set-answers
                            :payload update})))

(defn -add-answer [ds {:keys [id answer] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/insert! ds {:tname :answers
                    :values (->> answer
                                 (str/join ";;")
                                 (assoc {} :exercise-id id :text))})
    (update/apply exercise {:type :add-answer
                            :payload update})))

(defn -remove-answer [ds {:keys [id answer-id] :as update}]
  (when-let [exercise (-lookup ds {:id id})]
    (db/delete! ds {:tname :answers
                    :where [:= :id answer-id]})
    (update/apply exercise {:type :remove-answer
                            :payload update})))

(comment
  (def ds (db/ds :master))

  (-lookup ds {:id 1})
  (-find ds {:id 1})
  (-all ds)

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

  (-create ds {:unit-id 1
               :course-id 1
               :instruction "translate the following"
               :question-content "He is"
               :answer-type "bubbles"
               :options ["hy" "sy" "is"]
               :correct-message "correct!"
               :incorrect-message "o nei"
               :answers [["hy" "is"]]})

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
                    :answers [["is" "ek"]]})
  (-add-answer ds {:id 1
                   :answer ["hy" "is"]})
  (-remove-answer ds {:id 1
                      :answer-id 11})

  :end)
