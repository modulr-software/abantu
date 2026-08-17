(ns abantu.yolo.core
  (:require [abantu.yolo.ex-info :as ex]
            [ring.util.response :as res]
            [abantu.db.interface :as db]))




(defn create-context
  "This function produces anything you want from the http request and attaches it to the request.
   You can upgrade the request however you feel is necessary for the handle-command resolver to
   have the necessary context. The output of this function becomes the opts map passed to the handle-command function."
  [{:keys [user] :as req}]
  (cond-> req 
    true (assoc :ds (db/ds :master))
    (some? user) (assoc :student-ds (db/ds (db/db-name :student (:id user))))))

(defn- make-handler [{:keys [handler] :as _opts}]
  (fn [{:keys [body path-params] :as req}]
    (try
      (let [command (:command path-params)
            result (handler command body (create-context req))]
        (res/response result))
      (catch clojure.lang.ExceptionInfo e
        (let [{:keys [message] :as _details} (ex/ex-details e)
              code (ex/ex-code e)]
          (-> (res/response {:message message})
              (res/status code)))))))

(defn create-handler [{:keys [middleware] :as opts}]
  (-> (make-handler opts)
      (middleware)))