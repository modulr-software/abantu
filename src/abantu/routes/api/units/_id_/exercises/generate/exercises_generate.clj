(ns abantu.routes.api.units.-id-.exercises.generate.exercises-generate)

(defn get
  {:summary "generate exercises with an llm"
   :parameters {:path [:map [:id {:title "id"
                                  :description "whatever id"} :int]]
                :body [:map []]
                :query [:map []]}
   :responses {200 {:body [:map [:message :string]]}
               401 {:body [:map [:message :string]]}}}
  [{:keys [] :as _request}])
