(ns abantu.services.exercises.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.db.util :as db.util]
            [abantu.services.comments.interface :as comments]
            [abantu.services.exercises.core :as exercises]))

(defn- maybe [?schema]
  [:or ?schema :nil])

(def ?Option :string)
(def ?Answer [:map
              [:exercise-id :int]
              [:text :string]])

(def ?Exercise
  [:map
   [:id :int]
   [:unit-id :int]
   [:course-id :int]
   [:level :int]
   [:position :int]
   [:options [:vector ?Option]]
   [:answer-type :string]
   [:instruction :string]
   [:question-content :string]
   [:correct-message :string]
   [:answers [:vector ?Answer]]
   (maybe [:comments [:vector comments/?Comment]])])

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

(def ?SetPosition
  (mu/select-keys ?Exercise [:id :position]))

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
      (mu/assoc :answer [:vector :string])))

(def ?RemoveAnswer
  (-> (mu/select-keys ?Exercise [:id])
      (mu/assoc :answer-id :int)))

(def ?SetAnswers
  (-> (mu/select-keys ?Exercise [:id])
      (mu/assoc :answers [:vector [:vector :string]])))

(malt/defprotocol ExerciseQuery
  (lookup [input ?Lookup] ?Exercise)
  (find [input ?Find] [:vector ?Exercise])
  (all [] [:vector ?Exercise]))

(defn use-query
  ([] (use-query (db.util/conn)))
  ([ds]
   (malt/reify ExerciseQuery
     (lookup [_ input]
       (exercises/-lookup ds input))
     (find [_ input]
       (exercises/-find ds input))
     (all [_]
       (exercises/-all ds)))))

(malt/defprotocol ExerciseMutation
  (create [input (mu/dissoc ?Exercise :id)]
    (maybe ?Exercise))
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
  (set-position [input ?SetPosition]
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
     (create [_ input]
       (exercises/create ds input))
     (set-unit [_ input]
       (exercises/-set-unit ds input))
     (set-instruction [_ input]
       (exercises/-set-instruction ds input))
     (set-question-content [_ input]
       (exercises/-set-question-content ds input))
     (set-answer-type [_ input]
       (exercises/-set-answer-type ds input))
     (set-level [_ input]
       (exercises/-set-level ds input))
     (set-correct-message [_ input]
       (exercises/-set-correct-message ds input))
     (set-incorrect-message [_ input]
       (exercises/-set-incorrect-message ds input))
     (set-position [_ input]
       (exercises/-set-position ds input))
     (set-options [_ input]
       (exercises/-set-options ds input))
     (add-option [_ input]
       (exercises/-add-option ds input))
     (remove-option [_ input]
       (exercises/-remove-option ds input))
     (set-answers [_ input]
       (exercises/-set-answers ds input))
     (add-answer [_ input]
       (exercises/-add-answer ds input))
     (remove-answer [_ input]
       (exercises/-remove-answer ds input)))))
