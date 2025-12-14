(ns abantu.middleware.content-type
  (:require [abantu.util :as util]))

(defn wrap-content-type [handler]
  (fn [request]
    (let [response (handler request)
          content-type (util/content-type response)]
      (assoc
       response
       :headers
       (->
        (assoc (:headers response) "Content-Type" (or content-type "application/json"))
        (dissoc (:headers response) :content-type :Content-Type "content-type"))))))
