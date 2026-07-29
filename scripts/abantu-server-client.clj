#!/usr/bin/env bb
;; Minimal bencode nREPL client. Modeled on scripts/reload.clj.
;; Usage:
;;   bb scripts/abantu-server-client.clj load-file            # load dev/dev.clj
;;   bb scripts/abantu-server-client.clj eval "<code>"        # eval in dev ns
;;   bb scripts/abantu-server-client.clj start-server         # (server/start-server)
;;   bb scripts/abantu-server-client.clj stop-server          # (server/stop-server)
;;   bb scripts/abantu-server-client.clj running              # (server/running?)
(ns abantu-server-client
  (:require [bencode.core :as b]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.net Socket]
           [java.io ByteArrayOutputStream FileInputStream]))

(defn- port-file []
  (let [f (io/file ".nrepl-port")]
    (when (and (.exists f) (pos? (.length f))) f)))

(defn- read-port [^java.io.File f]
  (some-> (re-find #"\d+" (str/trim (slurp f))) parse-long))

(defn- ->str [x]
  (cond (nil? x) nil (string? x) x (bytes? x) (String. ^bytes x "UTF-8") :else (str x)))

(defn- send-msg! [^java.io.OutputStream out msg]
  (let [ba (ByteArrayOutputStream.)]
    (b/write-bencode ba msg)
    (.write out (.toByteArray ba)))
  (.flush out))

(defn- done-status? [m]
  (let [s (get m "status")]
    (and (sequential? s) (some #(= "done" (->str %)) s))))

(defn- read-resps [^java.io.InputStream in]
  (let [rdr (java.io.PushbackInputStream. in)]
    (loop [resps []]
      (let [msg (try (b/read-bencode rdr)
                     (catch Exception e
                       (if (instance? java.io.EOFException e) ::eof (throw e))))]
        (cond (identical? ::eof msg) resps
              (nil? msg) resps
              (done-status? msg) (conj resps msg)
              :else (recur (conj resps msg)))))))

(defn- report [resps]
  (let [outs    (keep #(-> % (get "out") ->str) resps)
        errs    (keep #(-> % (get "err") ->str) resps)
        status  (->> resps (keep #(get % "status")) (mapcat #(map ->str %)))
        ex      (some #(-> % (get "ex") ->str) resps)
        root-ex (some #(-> % (get "root-ex") ->str) resps)
        value   (some #(-> % (get "value") ->str) resps)
        err?    (or (some #{"eval-error" "syntax-error" "unknown-op"} status) (some? ex))]
    (when (seq outs) (print (str/join outs)))
    (when (seq errs) (binding [*out* *err*] (print (str/join errs))))
    (when value (println value))
    (when err?
      (when ex (binding [*out* *err*] (println "\n[client] eval error:" ex)))
      (when root-ex (binding [*out* *err*] (println "[client] root exception:" root-ex))))
    (flush)
    err?))

(defn- with-socket [port f]
  (with-open [sock (Socket. "localhost" (int port))]
    (f (.getInputStream sock) (.getOutputStream sock))))

(defn -main [& args]
  (let [pf (port-file)]
    (cond
      (not pf) (do (println "[client] no .nrepl-port; is nrepl running?") (System/exit 1))
      :else
      (let [port (read-port pf)]
        (if-not port
          (do (println "[client] could not parse .nrepl-port") (System/exit 1))
          (let [[cmd & rest] args
                err? (case cmd
                       "load-file"
                       (with-socket port
                         (fn [in out]
                           (send-msg! out {"op" "eval"
                                           "code" "(clojure.core/load-file \"dev/dev.clj\")"
                                           "ns" "user"})
                           (report (read-resps in))))
                       "eval"
                       (with-socket port
                         (fn [in out]
                           (send-msg! out {"op" "eval" "code" (str/join " " rest) "ns" "dev"})
                           (report (read-resps in))))
                       "start-server"
                       (with-socket port
                         (fn [in out]
                           (send-msg! out {"op" "eval" "code" "(server/start-server)" "ns" "dev"})
                           (report (read-resps in))))
                       "stop-server"
                       (with-socket port
                         (fn [in out]
                           (send-msg! out {"op" "eval" "code" "(server/stop-server)" "ns" "dev"})
                           (report (read-resps in))))
                       "running"
                       (with-socket port
                         (fn [in out]
                           (send-msg! out {"op" "eval" "code" "(server/running?)" "ns" "dev"})
                           (report (read-resps in))))
                       (do (binding [*out* *err*]
                             (println "[client] unknown command:" cmd))
                           true))]
            (System/exit (if err? 1 0))))))))

(apply -main *command-line-args*)
