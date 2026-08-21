(ns abantu.services.units.update)

#_{:clj-kondo/ignore [:redefined-var]}
(defmulti apply (fn [_ action] (:type action)))

(defmethod apply :create [unit {:keys [payload]}]
  (merge unit payload))

(defmethod apply :set-name [unit {:keys [payload]}]
  (assoc unit :name (:name payload)))

(defmethod apply :set-description [unit {:keys [payload]}]
  (assoc unit :description (:description payload)))

(defmethod apply :set-level [unit {:keys [payload]}]
  (assoc unit :level (:level payload)))

(defmethod apply :set-type [unit {:keys [payload]}]
  (assoc unit :type (:type payload)))

(defmethod apply :set-course-id [unit {:keys [payload]}]
  (assoc unit :course-id (:course-id payload)))

(defmethod apply :set-position [unit {:keys [payload]}]
  (assoc unit :position (:position payload)))
