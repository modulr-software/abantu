(ns abantu.async.interface
  (:require [abantu.email.gmail :as gmail]
            [abantu.util :as util]))

(def ^:private commands
  {:send-email {:schema [:map
                         [:to :string]
                         [:subject :string]
                         [:body :string]
                         [:type :keyword]]
                :handler #'gmail/send-email}})

(defn handle-command [command input]
  (if-let [{:keys [schema handler]} (commands command)]
    (let [validated (util/validate input schema)]
      (if-not (:success validated)
        (throw (ex-info (str "invalid input for " command "\n" (:error validated))
                        {:command command :input input}))
        (do (future (handler (:data validated))) ; ponytail: unbounded thread per call; swap to a bounded ExecutorService if throughput matters
            :accepted)))
    (throw (ex-info (str "unimplemented: " command) {:command command}))))

(comment
  (handle-command :send-email
                  {:to "jonte.puide@gmail.com"
                   :subject "your car's extended warranty is expired"
                   :body "hi this is serafe"
                   :type :text/plain})
  ())
