(ns abantu.services.exercises.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.db.util :as db.util]
            [abantu.services.exercises.core :as exercises]))

(defn- maybe [?schema]
  [:or ?schema :nil])

(def ?Option :string)
(def ?Answer :string)

(def ?Exercise
  [:map
   [:id :int]
   [:unit-id :int]
   [:course-id :int]
   [:level :int]
   [:order :int]
   [:options [:vector ?Option]]
   [:answer-type :string]
   [:instruction :string]
   [:question-content :string]
   [:correct-message :string]
   [:answers [:vector ?Answer]]])

(def ?Lookup
  (mu/select-keys ?Exercise [:id :unit-id :course-id]))

(def ?Find
  ?Lookup)

(def ?SetUnit
  (mu/select-keys ?Exercise [:id :unit-id]))

(def ?SetInstruction
  (mu/select-keys ?Exercise [:id :instruction]))

(def ?SetQuestionContent
  (mu/select-keys ?Exercise [:id :qestion-content]))

(def ?SetAnswerType
  (mu/select-keys ?Exercise [:id :answer-type]))

(def ?SetLevel
  (mu/select-keys ?Exercise [:id :level]))

(def ?SetCorrectMessage
  (mu/select-keys ?Exercise [:id :correct-message]))

(def ?SetIncorrectMessage
  (mu/select-keys ?Exercise [:id :incorrect-message]))

(def ?SetOrder
  (mu/select-keys ?Exercise [:id :order]))

(def ?SetOptions
  (mu/select-keys ?Exercise [:id :options]))

(def ?RemoveOption
  (-> (mu/select-keys ?Exercise [:id])
      (mu/assoc :option ?Option)))

(def ?AddOption
  (-> (mu/select-keys ?Exercise [:id])
      (mu/assoc :option ?Option)))

(def ?AddAnswer
  (-> (mu/select-keys ?Exercise [:id])
      (mu/assoc :answer ?Answer)))

(def ?RemoveAnswer
  (-> (mu/select-keys ?Exercise [:id])
      (mu/assoc :answer ?Answer)))

(def ?SetAnswers
  (mu/select-keys ?Exercise [:id :answers]))

(malt/defprotocol ExerciseQuery
  (lookup [input ?Lookup]
    [:vector ?Exercise])
  (find [input ?Find] [:vector ?Exercise])
  (all [] [:vector ?Exercise]))

(defn use-query
  ([] (use-query (db.util/conn)))
  ([ds]
   (malt/reify ExerciseQuery
     (lookup [_ input]
       (exercises/-lookup ds input))
     (find [_ input]
       )
     (all [_]
       (exercises/-all ds))
     )))

(malt/defprotocol ExerciseMutation
  (set-unit [input ?SetUnit]
    (maybe ?Exercise))
  (set-instruction [input ?SetInstruction]
    (maybe ?Exercise))
  (set-question-content [input ?SetQuestionContent]
    (maybe ?Exercise))
  (set-answer-type [input ?SetAnswerType]
    (maybe ?Exercise))
  (set-level [input ?SetLevel]
    (maybe ?Exercise))
  (set-correct-message [input ?SetCorrectMessage]
    (maybe ?Exercise))
  (set-incorrect-message [input ?SetIncorrectMessage]
    (maybe ?Exercise))
  (set-order [input ?SetOrder]
    (maybe ?Exercise))
  (set-options [input ?SetOptions]
    (maybe ?Exercise))
  (add-option [input ?AddOption]
    (maybe ?Exercise))
  (remove-option [input ?RemoveOption]
    (maybe ?Exercise))
  (set-answers [input ?SetAnswers]
    (maybe ?Exercise))
  (add-answer [input ?AddAnswer]
    (maybe ?Exercise))
  (remove-answer [input ?RemoveAnswer]
    (maybe ?Exercise)))


(defn use-mutation
  ([] (use-mutation (db.util/conn)))
  ([ds]
   (malt/reify ExerciseMutation
     (set-unit [_ input])
     (set-instruction [_ input])
     (set-question-content [_ input])
     (set-answer-type [_ input])
     (set-level [_ input])
     (set-correct-message [_ input])
     (set-incorrect-message [_ input])
     (set-order [_ input])
     (set-options [_ input])
     (add-option [_ input])
     (remove-option [_ input])
     (set-answers [_ input])
     (add-answer [_ input])
     (remove-answer [_ input]))))
