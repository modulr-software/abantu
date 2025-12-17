(ns abantu.routes.file)

(defn post [{:keys [multipart-params] :as _request}]
  (let [{:keys [tempfile]} (get multipart-params "hello")
        content (slurp tempfile)]
    {:status 200 :body {:message content}}))
