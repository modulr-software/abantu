(ns abantu.routes.openapi 
  (:require
    [abantu.routes.openapi :as openapi]
    [abantu.routes.openapi :as api]))

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

(def VocabByIdParams [:map [:id :string]])

(def IdPathParam [:map [:id :string]])

(def DeleteVocabResponse
  [:map [:message :string]])


;; HELPER FUNCTIONS


(defn error
  "A function that wraps an optional error data schema in a standard error message schema"
  ([] (error nil))
  ([data-schema]
   (vec
    (cond-> [:map [:message :string]]
      (some? data-schema)
      (concat [:data data-schema])))))


(defn- assoc-response [acc [key schema]]
  (assoc acc key {:body schema}))

(defn- assoc-param [acc [key schema]]
  (assoc acc key schema))

;; what i want: (WrapResponse {} 200 openapi/WrapError ...) => {200 {:body openapi/VocabSearchResult}}
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


(comment

{:summary "lkjdlfkjslkdjflksjdf"
 :parameters (params :path VocabByIdParams)
 :responses (-> (success VocabByIdResult)
                (not-found))}

  ;; THIS
  (-> (response 200 openapi/InsertVocabResponse)
      (response 404 (error InsertVocabParams)))

  (response  200 openapi/InsertVocabResponse)

  (error)
  ()
  )
