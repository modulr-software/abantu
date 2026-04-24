(ns abantu.routes.openapi
  (:require [malli.util :as mu]))

;; HELPER FUNCTIONS

(defn response-schema
  ([] (response-schema nil))
  ([data-schema]
   (vec
    (cond-> [:map [:message :string]]
      (some? data-schema)
      (concat [[:data data-schema]])))))

(defn error
  "A function that wraps an optional error data schema in a standard error message schema"
  ([] (error nil))
  ([data-schema]
   (response-schema data-schema)))

(defn- assoc-response [acc [key schema]]
  (assoc acc key {:body schema}))

(defn- assoc-param [acc [key schema]]
  (assoc acc key schema))

;; what i want: (api/response {} 200 (api/success)  404 (api/error)...) => {200 {:body openapi/VocabSearchResult}}
(defn response
  [& opts]
  (let [map-first? (map? (first opts))
        responses (if map-first? (first opts) {})
        opts (if map-first? (rest opts) opts)]
    (merge responses (reduce assoc-response {} (partition 2 opts)))))

(defn success
  "Returns a map that can be used as the responses field 
   for an openapi handler meta data. Optionally takes an existing
   openapi responses map as the first param and updates its 200 field
   and then returns it."
  ([schema] (success {} schema))
  ([responses data-schema]
   (response (if (map? responses) responses {})
             200
             data-schema)))

(defn not-found
  "Retuns a map that can be used as the responses field for an
   openapi handler meta data"
  ([] (not-found {} nil))
  ([schema-or-responses]
   (let [responses? (map? schema-or-responses)
         schema? (not responses?)
         responses (if responses? schema-or-responses nil)
         schema (if schema? schema-or-responses nil)]
     (not-found responses schema)))
  ([responses data-schema]
   (response (if (map? responses) responses {})
             404 (error data-schema))))

(defn bad-request
  "Retuns a map that can be used as the responses field for an
   openapi handler meta data"
  ([] (bad-request {} nil))
  ([schema-or-responses]
   (let [responses? (map? schema-or-responses)
         schema? (not responses?)
         responses (if responses? schema-or-responses nil)
         schema (if schema? schema-or-responses nil)]
     (bad-request responses schema)))
  ([responses data-schema]
   (response (if (map? responses) responses {})
             400 (error data-schema))))

(defn unauthenticated
  ([schema] (unauthenticated {} schema))
  ([responses schema]
   (response (if (map? responses) responses {})
             401 (error schema))))

(defn unauthorized
  ([schema] (unauthorized {} schema))
  ([responses schema]
   (response (if (map? responses) responses {})
             401 (error schema))))

(defn params
  "Returns a map of the openapi parameter schemas path, body, query.
   Optionally accepts an openapi parameters map as the first argument
   updating it and returning the result."
  [& opts]
  (let [map-first? (map? (first opts))
        parameters (if map-first? (first opts) {})
        opts (if map-first? (rest opts) opts)]
    (merge parameters (reduce assoc-param {} (partition 2 opts)))))

(defn- sometimes-entry [[k _ s]] [k {:optional true} [:maybe s]])
(defn- maybe-keys [schema]
  (mu/transform-entries
   schema
   #(mapv sometimes-entry %)))

(comment

  {:summary "lkjdlfkjslkdjflksjdf"
   :parameters (params :path VocabByIdParams)
   :responses (-> (success VocabByIdResult)
                  (not-found))}

  ;; THIS
  (-> (response 200 InsertVocabResponse)
      (response 404 (error InsertVocabParams)))

  (response  200 InsertVocabResponse)

  (error)
  ())

;; MALLI SCHEMAS

(defn optional [key type]
  [key {:optional true} type])

(defn maybe [type]
  [:maybe type])

(defn sometimes [key type]
  (optional key (maybe type)))

(def VocabSearchParams
  [:map
   (sometimes :type :string)
   (sometimes :search :string)])

(def VocabSearchResult
  [:map
   [:id :int]
   [:xhosa :string]
   [:english :string]
   (sometimes :illustration :string)
   (sometimes :noun-class :string)
   [:type :string]])

(def VocabSearchResponse
  [:vector VocabSearchResult])

(def InsertVocab
  [:map
   [:xhosa :string]
   [:english :string]
   (sometimes :illustration :string)
   (sometimes :noun-class :string)
   [:type :string]])

(def InsertVocabParams
  [:vector InsertVocab])

(def InsertVocabResponse
  [:vector VocabSearchResult])

(def VocabByIdResult
  VocabSearchResult)

(def UpdateVocabBody
  [:map
   (sometimes :xhosa :string)
   (sometimes :english :string)
   (sometimes :illustration :string)
   (sometimes :noun-class :string)
   (sometimes :type :string)])

(def VocabByIdParams [:map [:id :int]])

(def IdPathParam [:map [:id :int]])

(def DeleteVocabResponse
  [:map [:message :string]])

(def User
  [:map
   [:id :int]
   [:email :string]
   (sometimes :firstname :string)
   (sometimes ::lastname :string)
   [:email-verified :boolean]
   (sometimes :mobile :string)
   (sometimes :profile-image :string)
   [:role :string]])

(def GetUnitResult
  [:map
   [:id :int]
   [:course-id :int]
   [:name :string]
   [:description :string]
   [:type [:enum "lesson" "practice"]]
   [:level :int]])

(def GetUnitResponse
  GetUnitResult)

(def GetUnitsResponse
  [:vector GetUnitResult])

(def CreateUnitParam
  [:map
   [:name :string]
   [:description :string]
   [:type [:enum "lesson" "practice"]]
   [:level :int]])

(def CreateUnitsParam
  [:vector CreateUnitParam])

(def CreateUnitsResponse
  [:map
   [:message :string]
   [:data [:vector GetUnitResult]]])

(def AnswerParam
  [:map
   [:text {:optional true} [:maybe [:or [:vector :string] :string]]]
   (sometimes :audio :string)])

(def AnswerParams
  [:vector AnswerParam])

(def ExerciseParam
  [:map
   [:instruction :string]
   [:question-content :string]
   [:course-id :int]
   (sometimes :audio :string)
   [:answer-type [:enum "freetext" "bubbles"]]
   [:options [:vector :string]]
   (sometimes :answers AnswerParams)])

(def ExerciseParams
  [:vector ExerciseParam])

(def GetExerciseResult
  [:map
   [:id :int]
   [:unit-id :int]
   [:instruction :string]
   [:question-content :string]
   [:answer-type [:enum "freetext" "bubbles"]]
   (sometimes :audio :string)
   [:options [:vector :string]]
   [:answers AnswerParams]])

(def GetExercisesResponse
  [:vector GetExerciseResult])

(def UpdateExerciseParam
  [:map
   (sometimes :unit-id :int)
   (sometimes :instruction :string)
   (sometimes :question-content :string)
   (sometimes :audio :string)
   (sometimes :answer-type [:enum "freetext" "bubbles"])
   (sometimes :options [:vector :string])
   (sometimes :answers AnswerParams)])

(def UpdateUnitParam
  (-> GetUnitResult
      (mu/optional-keys)
      (mu/dissoc :creator-id)))

(def CourseStatus :string)
(def
  ^{:tech/debt "creator key should be required once user auth exists"}
  GetCourseResponse
  [:map
   [:id :int]
   [:name :string]
   [:language :string]
   [:status CourseStatus]
   (sometimes :creator User)
   [:units GetUnitsResponse]])

(def GetCoursesResponse
  [:vector GetCourseResponse])

(def CreateCourseParam
  [:map
   [:name :string]
   [:language :string]
   [:units CreateUnitsParam]])

(def UpdateCourseParam
  [:map
   (sometimes :name :string)
   (sometimes :language :string)
   (sometimes :status CourseStatus)
   (sometimes :creator-id :int)])

(def RegisterStudentParams
  [:map
   [:email :string]
   [:password :string]
   [:confirm-password :string]
   [:device-uuid :string]])

(def RegisterStudentResponse
  [:map
   [:access-token :string]
   [:refresh-token :string]])

(def LoginParams
  [:map
   [:email :string]
   [:password :string]])

(def LoginResponse
  [:map
   [:access-token :string]
   [:refresh-token :string]])

(def EmailVerificationParams
  [:map
   [:email-hash :string]])

(def StartSessionResponse
  [:map
   [:session-id :int]
   [:exercises GetExercisesResponse]
   [:level :int]])

(def EndSessionParams
  [:map
   [:session-id :int]
   [:answers [:vector
              [:map
               [:exercise-id :int]
               [:answer [:or [:vector :string] :string]]
               [:started-at :string]
               [:ended-at :string]]]]])
