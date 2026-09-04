(ns abantu.services.courses.update)

#_{:clj-kondo/ignore [:redefined-var]}
(defmulti apply (fn [_ action] (:type action)))

(defmethod apply :create [course {:keys [payload]}]
  (merge course payload))

(defmethod apply :delete [_course {:keys [_payload]}]
  nil)

(defmethod apply :set-name [course {:keys [payload]}]
  (assoc course :name (:name payload)))

(defmethod apply :set-language [course {:keys [payload]}]
  (assoc course :language (:language payload)))

(defmethod apply :set-description [course {:keys [payload]}]
  (assoc course :description (:description payload)))

(defmethod apply :set-publishable [course {:keys [payload]}]
  (assoc course :publishable (:publishable payload)))

(defmethod apply :set-visible [course {:keys [payload]}]
  (assoc course :visible (:visible payload)))

(defmethod apply :set-review-pending [course {:keys [payload]}]
  (assoc course :review-pending (:review-pending payload)))

(defmethod apply :set-creator-id [course {:keys [payload]}]
  (assoc course :creator-id (:creator-id payload)))
