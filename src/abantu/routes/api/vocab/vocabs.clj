(ns abantu.routes.api.vocab.vocabs
  (:require [abantu.services.interface :as services]
            [ring.util.response :as res]
            [clj-fuzzy.levenshtein :as levenshtein]
            [abantu.routes.openapi :as openapi]
            [abantu.routes.openapi :as api]))


(defn get
  {:summary "get all the vocab n shit"
   :parameters (api/params :query api/VocabSearchParams) 
   :responses (api/success api/VocabSearchResponse)}
  [{:keys [ds query-params] :as _request}]
  (let [{:keys [type search]} query-params
        vocab (services/vocab-query ds {:type type})
        searched (if (some? search)
                   (->>
                    (mapv (fn [{:keys [xhosa english] :as v}]
                            (let [len (levenshtein/distance search english)
                                  lxh (levenshtein/distance search xhosa)]
                              (assoc v :levenshtein (if (< len lxh) len lxh)))) vocab)
                    (sort-by :levenshtein)
                    (mapv #(dissoc % :levenshtein)))
                   vocab)]
    (res/response searched)))

(defn post
  {:summary "insert all the vocab lol"
   :parameters (api/params :body api/InsertVocabParams)
   :responses (-> (api/success api/InsertVocabResponse)
                  (api/bad-request))}
  [{:keys [ds body] :as _request}]
  (let [new-word (services/insert-vocab! ds {:data body})]
    (-> (res/response new-word)
        (res/status 201))))

(comment
  (require '[abantu.db.util :as db.util])

  (def ds (db.util/conn))
  (def search "suk")
  (def vocab (services/vocab-query ds {}))

  (->>
   (mapv (fn [{:keys [xhosa english] :as v}]
           (let [len (levenshtein/distance search english)
                 lxh (levenshtein/distance search xhosa)]
             (assoc v :levenshtein (if (< len lxh) len lxh)))) vocab)
   (sort-by :levenshtein)
   (vec)
   (mapv #(dissoc % :levenshtein)))

  ())
