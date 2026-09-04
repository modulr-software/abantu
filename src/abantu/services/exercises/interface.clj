(ns abantu.services.exercises.interface
  (:require [abantu.util :as util]
            [io.julienvincent.malt :as malt]
            [malli.util :as mu]
            [abantu.db.util :as db.util]
            [abantu.services.comments.interface :as comments]
            [abantu.services.exercises.core :as exercises]))

(def ?Option :string)
(def ?Answer
  [:map
   [:id :int]
   [:text [:vector :string]]
   [:exercise-id :int]])

(def ?Exercise
  [:map
   [:id :int]
   [:unit-id :int]
   [:course-id :int]
   [:level :int]
   [:position {:optional true} [:maybe :int]]
   [:options [:vector ?Option]]
   [:answer-type [:enum "freetext" "bubbles"]]
   [:instruction :string]
   [:question-content :string]
   [:correct-message {:optional true} [:maybe :string]]
   [:incorrect-message {:optional true} [:maybe :string]]
   [:answers [:vector ?Answer]]
   [:comments (util/maybe [:vector comments/?Comment])]])

(def ?Lookup
  [:or
   [:map [:id :int]]
   [:map [:unit-id :int] [:course-id {:optional true} :int]]
   [:map [:course-id :int]]])

(def ?Find
  ?Lookup)

(def ?Create
  (-> (mu/dissoc ?Exercise :id)
      (mu/dissoc :comments)
      (mu/update-entry-properties :level assoc :optional true)
      (mu/update-entry-properties :options assoc :optional true)
      (mu/assoc :answers [:vector [:vector :string]])))

(def ?SetUnit
  (mu/select-keys ?Exercise [:id :unit-id]))

(def ?SetInstruction
  (mu/select-keys ?Exercise [:id :instruction]))

(def ?SetQuestionContent
  (mu/select-keys ?Exercise [:id :question-content]))

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
  (lookup [input ?Lookup] (util/maybe ?Exercise))
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
  (create [input ?Create]
    (util/maybe ?Exercise))
  (delete [input ?Lookup]
    :nil)
  (set-unit [input ?SetUnit]
    (util/maybe ?Exercise))
  (set-instruction [input ?SetInstruction]
    (util/maybe ?Exercise))
  (set-question-content [input ?SetQuestionContent]
    (util/maybe ?Exercise))
  (set-answer-type [input ?SetAnswerType]
    (util/maybe ?Exercise))
  (set-level [input ?SetLevel]
    (util/maybe ?Exercise))
  (set-correct-message [input ?SetCorrectMessage]
    (util/maybe ?Exercise))
  (set-incorrect-message [input ?SetIncorrectMessage]
    (util/maybe ?Exercise))
  (set-position [input ?SetPosition]
    (util/maybe ?Exercise))
  (set-options [input ?SetOptions]
    (util/maybe ?Exercise))
  (add-option [input ?AddOption]
    (util/maybe ?Exercise))
  (remove-option [input ?RemoveOption]
    (util/maybe ?Exercise))
  (set-answers [input ?SetAnswers]
    (util/maybe ?Exercise))
  (add-answer [input ?AddAnswer]
    (util/maybe ?Exercise))
  (remove-answer [input ?RemoveAnswer]
    (util/maybe ?Exercise)))

(defn use-mutation
  ([] (use-mutation (db.util/conn)))
  ([ds]
   (malt/reify ExerciseMutation
     (create [_ input]
       (exercises/-create ds input))
     (delete [_ input]
       (exercises/-delete ds input))
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

(comment
  (require '[abantu.db.interface :as db])
  (def ds (db/ds :master))

  (let [data (exercises/-all ds)]
    (malli.error/humanize (malli.core/explain [:vector ?Exercise] data)))

  (->>
   (exercises/-all ds)
   (mapcat #(if (seq (:comments %))
              (:comments %)))
   (filterv identity)
   (filterv #(seq (:user %))))

  (let [eq (use-query ds)
        id 1091
        lookup' (lookup eq {:id id})
        find' (find eq {:id id})
        all' (all eq)]
    all'
    #_(all eq))

  (def em (use-mutation))

  (create em {:unit-id 1
              :course-id 1
              :instruction "translate the following"
              :question-content "who are you"
              :answer-type "bubbles"
              :options ["wat" "wie" "hoe" "is" "jy"]
              :correct-message "correct!"
              :incorrect-message "o nei"
              :answers [["wie" "is" "jy"]]})

  (set-unit em {:id 1
                :unit-id 2})
  (set-instruction em {:id 1
                       :instruction "Translate the following:"})
  (set-question-content em {:id 1
                            :question-content "Who are you?"})
  (set-answer-type em {:id 1
                       :answer-type "bubbles"})
  (set-level em {:id 1
                 :level 2})
  (set-correct-message em {:id 1
                           :correct-message "that's so crazy guys"})
  (set-incorrect-message em {:id 1
                             :incorrect-message "bro that was so lame"})
  (set-position em {:id 1
                    :position 2})
  (set-options em {:id 1
                   :options ["wie" "jy" "hoe" "is" "ek"]})
  (add-option em {:id 1
                  :option "weet"})
  (remove-option em {:id 1
                     :option "weet"})
  (set-answers em {:id 1
                   :answers [["wie" "is" "jy"]]})
  (add-answer em {:id 1
                  :answer ["hoe" "is" "jy"]})
  (remove-answer em {:id 1
                     :answer-id 30})

  :end)
