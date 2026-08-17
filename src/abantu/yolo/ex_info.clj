(ns abantu.yolo.ex-info)

(def ^:private codes
  {:not-found 404
   :internal-server-error 500
   :unauthenticated 401
   :unauthorized 403
   :im-a-teapot 418})

(defn- validate-code [code]
  (cond
    (integer? code) code
    (nil? code) 500
    (or (not (keyword? code))
        (not (some? (get-in codes [code]))))
    (throw (ex-info (str  "Invalid error code passed to ex-yolo: " code)
                    {:code (get-in codes [code])}))
    :else 500))

(defn ex-yolo [message data]
  (let [{:keys [code]} data
        code' (validate-code code)]
    (ex-info message {:code code'})))

(defn ex-details [e]
  {:message (ex-message e)
   :data (ex-data e)})

(defn ex-code [e]
  (get-in (ex-details e) [:data :code]))

(defn code [c]
  (get-in codes [c]))