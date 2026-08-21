(ns abantu.async.interface
  (:require [abantu.email.gmail :as gmail]
            [abantu.util :as util]
            [abantu.logger :as logger]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn handler-result [& {:as command-result}]
  command-result)

(defn event [name data]
  {:data data
   :name name})

(defn events [& {:as events'}]
  (mapv (fn [[k v]] (event k v)) events'))

(defn action [name input]
  {:type :action
   :input input
   :name name})

(defn actions [& {:as actions'}]
  (mapv (fn [[k v]] (action k v)) actions'))

(defn gmail-schema []
  [:map
   [:to :string]
   [:subject :string]
   [:body :string]
   [:type :keyword]])

(defn send-email! [ctx]
  (handler-result :actions (actions :send-email ctx)))

(def ^:private cmd-register
  {:notify {:schema {:in (gmail-schema)
                     :out :nil}
            :handler send-email!
            :actions [:send-email]
            :events [:notification-scheduled]}
   :test-command {:schema {:in :string
                           :out :nil}
                  :handler (fn [msg] {:data msg :actions [{:type :log
                                                           :input msg}]})}})
(def ^:private action-register
  {:log {:schema {:in :string
                  :out :nil}
         :handler logger/log-command!}
   :send-email {:schema {:in (gmail-schema) 
                         :out :nil}
                :handler gmail/send-email}})

(defn- get-command [commands name]
  (when-let [cmd (get-in commands [name])]
    (assoc cmd :name name :type :command)))

(defn- get-action [actions name]
  (when-let [action (get-in actions [name])]
    (assoc action :name name :type :action)))

(defn- throw-on-unimplemented! [name type config]
  (when-not (some? config) 
      (throw (ex-info (str "no implementation for " (if (= type :action) "action" "command") ": " name)
                      {:name name
                       :type type}))))

(defn- throw-on-invalid-input! [{:keys [schema input name type]}]
  (let [{:keys [data error success]} (util/validate input (:in schema))]
    (when (and (some? (:in schema)) (not success))
      (throw (ex-info (str "Invalid input for " (if (= type :action) "action" "command") ": " name "\n" error)
                      {:name name
                       :type :action
                       :error error
                       :input data
                       :data input})))))

(defn- throw-on-unregistered-event! [config result]
  (let [events (-> (:events config) set) 
        events' (->> (:events result) (mapv :name) set) 
        intersection (set/intersection events events')
        diff (set/difference events' intersection)]
    (prn "diff" diff)
    (prn "diff" (seq diff))
    (when (seq diff)
      (throw (ex-info (str (if (= (:type config) :action) "Action" "Command") " " (:name config) " emitted unknown events: " (str/join ", " (seq diff)))
                      {:events diff
                       :allowed-events events
                       :name name
                       :type (:type config)})))))

(defn- throw-on-unregistered-action! [config result]
  (let [actions (-> (:actions config) set)
        actions' (->> (:actions result) (mapv :name) set)
        intersection (set/intersection actions actions')
        diff (set/difference actions' intersection)]
    (when (seq diff)
      (throw (ex-info (str "Actions " (str/join ", " (seq diff)) " is not registered on command " (:name config))
                      {:type :action
                       :names (seq diff)})))))

(defn- action-worker [action input]
  (let [{:keys [handler] :as config} (get-action action-register action)]
    (throw-on-unimplemented! action :action config)
    (throw-on-invalid-input! (assoc config :input input))
    (let [action-result (handler input)]
      (:data action-result))))


(defn handle-action [action input]
  (future (action-worker action input)))

(defn handle-command [command input]
  (let [{:keys [handler] :as cmd} (get-command cmd-register command)]
    (throw-on-unimplemented! command :command cmd)
    (throw-on-invalid-input! (assoc cmd :name command :input input))
    (println "Running command:" command)
    (let [{:keys [actions events' data] :as command-result} (handler input)]
      (throw-on-unregistered-event! cmd command-result)
      ;;validation of events
      (run! #(handle-action :log (name %)) events')
      ;;spawn action handlers
      (throw-on-unregistered-action! cmd command-result)
      (run! #(handle-action (:name %) (:input %)) actions)
      (println "Finished running command:" command)
      data)))


(comment
  ;; should pass if awaited with nil and with verifiably sent email
  @(handle-action :send-email {:to "keaganncollins@gmail.com"
                               :subject "Bitte"
                               :body "Verpissen sie sich"
                               :type :text/plain})


  ;; doesnt need to be awaited, should pass with verifiably sent email
  (action-worker :send-email {:to "jonte.puide@gmail.com"
                              :subject "Bitte"
                              :body "Verpissen sie sich"
                              :type :text/plain})


  ;; should pass, not awaitable side effect, with verifiably sent email
  (handle-command :notify-user
                  {:to "jonte.puide@gmail.com"
                   :subject "Bitte"
                   :body "Verpissen sie sich"
                   :type :text/plain})


  ;; should fail due to invalid schema
  (throw-on-invalid-input! {:schema {:in :string
                                     :out :nil}
                            :input 10
                            :name :test
                            :type :command})

  ;;expected to pass with nil
  (throw-on-unimplemented! :notify-user :command {:name :test
                                                  :type :command
                                                  :handler (fn [_] (prn "message"))
                                                  :input "this is message"})
  ;; expected to fail
  (throw-on-unimplemented! :notify-user :command nil)

  ;;expected to pass with nil
  (throw-on-unregistered-event! {:name :test
                                 :events [:tested]
                                 :actions [:log]}
                                (handler-result :events [(event :tested {})]))

  ;;expected to be wrong
  (throw-on-unregistered-event! {:name :test
                                 :events [:tested]
                                 :actions [:log]}
                                (handler-result :events [(event :wrong {})]))

  (throw-on-unregistered-action! {:name :test
                                  :actions [:log]}
                                 (handler-result :actions [(action :log {})]))
  ())

