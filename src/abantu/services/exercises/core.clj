(ns abantu.services.exercises.core
  (:require [abantu.db.interface :as db]
            [abantu.services.comments :as comments]
            [abantu.services.exercises.update :as update]
            [honey.sql.helpers :as h]
            [clojure.string :as str]
            [abantu.util :as util]))

(defn- process-options [exercise]
  (-> exercise
      (dissoc :audio) ;; deprecated field, never returned
      (update-in
       [:options]
       #(or (when (seq %)
              (-> (clojure.string/split (or % "") #";;")
                  (vec)))
            []))))

(defn- process-answer [{:keys [text]}]
  (if (clojure.string/includes? text ";;")
    (clojure.string/split text #";;")
    [text]))

(defn- save-answers-for-exercise! [ds exercise-id answers]
  (db/delete! ds {:tname :answers
                  :where [:= :exercise-id exercise-id]})
  (if (seq answers)
    (->> answers
         (mapv #(assoc {} :text (str/join ";;" %) :exercise-id exercise-id))
         (assoc {:tname :answers :ret :*} :data)
         (db/insert! ds)
         (mapv process-answer))
    []))

(defn- attach-answers [ds {:keys [id] :as exercise}]
  (assoc exercise :answers (->> (db/find ds {:tname :answers
                                             :where [:= :exercise-id id]
                                             :ret :*})
                                (mapv process-answer))))

(defn- attach-comments [ds {:keys [id] :as exercise}]
  (assoc exercise :comments
         (comments/get-for-exercise ds id "all" #(db/ds :master))))

(defn -lookup  [ds {:keys [id uuid unit-id course-id]}]
  (when-let [exercise (db/find ds (cond-> {:tname :exercises
                                           :ret :1}
                                    (some? uuid) (h/where [:= :uuid uuid])
                                    (some? id) (h/where [:= :id id])
                                    (some? unit-id) (h/where [:= :unit-id unit-id])
                                    (some? course-id) (h/where [:= :course-id course-id])))]
    (->> exercise
         (attach-comments ds)
         (process-options)
         (attach-answers ds))))

(defn -all [ds]
  (->> (db/find ds {:tname :exercises
                    :ret :*})
       (mapv (comp (partial attach-comments ds)
                   process-options
                   (partial attach-answers ds)))))

(defn -find [ds {:keys [id uuid unit-id course-id]}]
  (->> (db/find ds (cond-> {:tname :exercises
                            :ret :*}
                     (some? uuid) (h/where [:= :uuid uuid])
                     (some? id) (h/where [:= :id id])
                     (some? unit-id) (h/where [:= :unit-id unit-id])
                     (some? course-id) (h/where [:= :course-id course-id])))
       (mapv (comp (partial attach-comments ds)
                   process-options
                   (partial attach-answers ds)))))

;; resolved lazily: changes/interface requires changes/migrate, which requires
;; exercises/interface, so a static require from here is a cyclic load dependency
(def ^:private add-exercise-update!
  (delay (requiring-resolve 'abantu.services.changes.interface/add-exercise-update!)))

(defn- add-change!
  "records an exercise change through the changes api, when one was given. the
   exercises table only stores the unit and course ids, so resolve the uuids
   that exercise-changes requires from them."
  [changes-api ds change-type {:keys [uuid unit-id course-id]} payload]
  (when changes-api
    (@add-exercise-update!
     changes-api
     {:change-type change-type
      :update (merge payload
                     {:uuid uuid
                      :unit-uuid (:uuid (db/find-one ds {:tname :units
                                                         :where [:= :id unit-id]}))
                      :course-uuid (:uuid (db/find-one ds {:tname :courses
                                                          :where [:= :id course-id]}))})})))

(defn -create
  [changes-api ds {:keys [uuid options answers] :as update}]
  (let [update' (assoc update :options (str/join ";;" options))
        {:keys [id]} (db/insert! ds {:tname :exercises
                                     :values (-> (dissoc update' :answers)
                                                 (assoc :uuid (or uuid (util/uuid))))
                                     :ret :1})
        answers' (->> (mapv #(str/join ";;" %) answers)
                      (mapv #(assoc {} :exercise-id id :text %)))
        _ (db/insert! ds {:tname :answers
                          :values answers'})
        exercise (-lookup ds {:id id})
        applied (update/apply exercise {:type :create
                                        :payload update})]
    (add-change! changes-api ds "create" exercise applied)
    applied))

(defn -delete
  [changes-api ds {:keys [id uuid] :as update}]
  (let [exercise (-lookup ds {:id id :uuid uuid})
        {:keys [id]} exercise
        _ (db/delete! ds {:tname :comments
                          :where [:= :exercise-id id]})
        _ (db/delete! ds {:tname :answers
                          :where [:= :exercise-id id]})
        _ (db/delete! ds {:tname :exercises-completed
                          :where [:= :exercise-id id]})
        _ (db/delete! ds {:tname :exercises
                          :where [:= :id id]})]
    (add-change! changes-api ds "delete" exercise update)
    (update/apply nil {:type :delete
                       :payload update})))

(defn -set-unit [changes-api ds {:keys [id uuid unit-id] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:unit-id unit-id}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-unit" (assoc exercise :unit-id unit-id) update)
    (update/apply exercise {:type :set-unit
                            :payload update})))

(defn -set-instruction [changes-api ds {:keys [id uuid instruction] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:instruction instruction}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-instruction" exercise update)
    (update/apply exercise {:type :set-instruction
                            :payload update})))

(defn -set-question-content [changes-api ds {:keys [id uuid question-content] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:question-content question-content}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-question-content" exercise update)
    (update/apply exercise {:type :set-question-content
                            :payload update})))

(defn -set-answer-type [changes-api ds {:keys [id uuid answer-type] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:answer-type answer-type}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-answer-type" exercise update)
    (update/apply exercise {:type :set-answer-type
                            :payload update})))

(defn -set-level [changes-api ds {:keys [id uuid level] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:level level}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-level" exercise update)
    (update/apply exercise {:type :set-level
                            :payload update})))

(defn -set-correct-message [changes-api ds {:keys [id uuid correct-message] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:correct-message correct-message}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-correct-message" exercise update)
    (update/apply exercise {:type :set-correct-message
                            :payload update})))

(defn -set-incorrect-message [changes-api ds {:keys [id uuid incorrect-message] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:incorrect-message incorrect-message}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-incorrect-message" exercise update)
    (update/apply exercise {:type :set-incorrect-message
                            :payload update})))

(defn -set-position [changes-api ds {:keys [id uuid position] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:position position}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-position" exercise update)
    (update/apply exercise {:type :set-position
                            :payload update})))

(defn -set-options [changes-api ds {:keys [id uuid options] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" options)}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "set-options" exercise update)
    (update/apply exercise {:type :set-options
                            :payload update})))

(defn -add-option [changes-api ds {:keys [id uuid option] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" (conj (:options exercise) option))}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "add-option" exercise update)
    (update/apply exercise {:type :add-option
                            :payload update})))

(defn -remove-option [changes-api ds {:keys [id uuid option] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (db/update! ds {:tname :exercises
                    :values {:options (str/join ";;" (update/remove-first (:options exercise) option))}
                    :where [:= :id (:id exercise)]})
    (add-change! changes-api ds "remove-option" exercise update)
    (update/apply exercise {:type :remove-option
                            :payload update})))

(defn -set-answers [changes-api ds {:keys [id uuid answers] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (let [applied (update/apply exercise {:type :set-answers
                                          :payload (assoc update
                                                          :answers (save-answers-for-exercise! ds (:id exercise) answers))})]
      (add-change! changes-api ds "set-answers" exercise update)
      applied)))

(defn -add-answer [changes-api ds {:keys [id uuid answer] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (let [inserted (db/insert! ds {:tname :answers
                                   :values {:text       (str/join ";;" answer)
                                            :exercise-id (:id exercise)}
                                   :ret :1})
          applied (update/apply exercise {:type :add-answer
                                          :payload (assoc update
                                                          :answer (process-answer inserted))})]
      (add-change! changes-api ds "add-answer" exercise update)
      applied)))

(defn -remove-answer [changes-api ds {:keys [id uuid answer-id] :as update}]
  (when-let [exercise (-lookup ds {:id id :uuid uuid})]
    (let [_ (db/delete! ds {:tname :answers
                            :where [:= :id answer-id]})
          applied (update/apply exercise {:type :remove-answer
                                          :payload update})]
      (add-change! changes-api ds "remove-answer" exercise update)
      applied)))

(comment
  (def ds (db/ds :master))

  (-lookup ds {:id 1091})
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

  (-create nil ds {:unit-id 1
               :course-id 1
               :instruction "translate the following"
               :question-content "He is"
               :answer-type "bubbles"
               :options ["hy" "sy" "is"]
               :correct-message "correct!"
               :incorrect-message "o nei"
               :answers [["hy" "is"]]})

  (-set-unit nil ds {:id 1
                 :unit-id 2})
  (-set-instruction nil ds {:id 1
                        :instruction "Translate the following:"})
  (-set-question-content nil ds {:id 1
                             :question-content "I am"})
  (-set-answer-type nil ds {:id 1
                        :answer-type "bubbles"})
  (-set-level nil ds {:id 1
                  :level 2})
  (-set-correct-message nil ds {:id 1
                            :correct-message "wow amazing!"})
  (-set-incorrect-message nil ds {:id 1
                              :incorrect-message "bro that was so lame"})
  (-set-position nil ds {:id 1
                     :position 1})
  (-set-options nil ds {:id 1
                    :options ["ek" "hy" "sy" "is"]})
  (-add-option nil ds {:id 1
                   :option "weet"})
  (-remove-option nil ds {:id 1
                      :option "weet"})
  (-set-answers nil ds {:id 1
                    :answers [["is" "ek"]]})
  (-add-answer nil ds {:id 1
                   :answer ["hy" "is"]})
  (-remove-answer nil ds {:id 1
                      :answer-id 11})

  :end)
