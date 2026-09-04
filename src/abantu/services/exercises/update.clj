(ns abantu.services.exercises.update)

(defn remove-first [v target]
  (let [[before [_ & after]] (split-with #(not= % target) v)]
    (vec (concat before after))))

#_{:clj-kondo/ignore [:redefined-var]}
(defmulti apply (fn [_ action] (:type action)))

(defmethod apply :create [exercise {:keys [payload]}]
  (merge exercise (dissoc payload :answers)))

(defmethod apply :delete [_exercise {:keys [_payload]}]
  nil)

(defmethod apply :set-unit [exercise {:keys [payload]}]
  (assoc exercise :unit-id (:unit-id payload)))

(defmethod apply :set-instruction [exercise {:keys [payload]}]
  (assoc exercise :instruction (:instruction payload)))

(defmethod apply :set-question-content [exercise {:keys [payload]}]
  (assoc exercise :question-content (:question-content payload)))

(defmethod apply :set-answer-type [exercise {:keys [payload]}]
  (assoc exercise :answer-type (:answer-type payload)))

(defmethod apply :set-level [exercise {:keys [payload]}]
  (assoc exercise :level (:level payload)))

(defmethod apply :set-correct-message [exercise {:keys [payload]}]
  (assoc exercise :correct-message (:correct-message payload)))

(defmethod apply :set-incorrect-message [exercise {:keys [payload]}]
  (assoc exercise :incorrect-message (:incorrect-message payload)))

(defmethod apply :set-position [exercise {:keys [payload]}]
  (assoc exercise :position (:position payload)))

(defmethod apply :set-options [exercise {:keys [payload]}]
  (assoc exercise :options (:options payload)))

(defmethod apply :add-option [exercise {:keys [payload]}]
  (assoc exercise :options (conj (:options exercise) (:option payload))))

(defmethod apply :remove-option [exercise {:keys [payload]}]
  (assoc exercise :options (remove-first (:options exercise) (:option payload))))

(defmethod apply :set-answers [exercise {:keys [payload]}]
  ;; payload :answers are already processed rows with ids (see -set-answers)
  (assoc exercise :answers (:answers payload)))

(defmethod apply :add-answer [exercise {:keys [payload]}]
  ;; payload :answer is already a processed row with id (see -add-answer)
  (assoc exercise :answers (conj (:answers exercise) (:answer payload))))

(defmethod apply :remove-answer [{:keys [answers] :as exercise} {:keys [payload]}]
  (assoc exercise :answers (vec (remove #(= (:id %) (:answer-id payload)) answers))))
