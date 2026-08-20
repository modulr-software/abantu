(ns abantu.services.units.interface
  (:require [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.db.util :as db.util]))

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

(malt/defprotocol UnitQuery
  (lookup [input ?Lookup]
    ?Exercise)
  (find [input ?Find]
    [:vector ?Exercise]))

(malt/defprotocol UnitMutation
  (set-unit [input ?SetUnit]
    :boolean)
  (set-instruction [input ?SetInstruction]
    :boolean)
  (set-question-content [input ?SetQuestionContent]
    :boolean)
  (set-answer-type [input ?SetAnswerType]
    :boolean)
  (set-level [input ?SetLevel]
    :boolean)
  (set-correct-message [input ?SetCorrectMessage]
    :boolean)
  (set-incorrect-message [input ?SetIncorrectMessage]
    :boolean)
  (set-order [input ?SetOrder]
    :boolean)
  (set-options [input ?SetOptions]
    :boolean)
  (add-option [input ?AddOption]
    :boolean)
  (remove-option [input ?RemoveOption]
    :boolean)
  (set-answers [input ?SetAnswers]
    :boolean)
  (add-answer [input ?AddAnswer]
    :boolean)
  (remove-answer [input ?RemoveAnswer]
    :boolean))

(defn use-query
  ([] (use-query (db.util/conn)))
  ([ds]
   (malt/reify UnitQuery
     (lookup [_ input])
     (find [_ input]))))

(defn use-mutation
  ([] (use-mutation (db.util/conn)))
  ([ds]
   (malt/reify UnitMutation
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
