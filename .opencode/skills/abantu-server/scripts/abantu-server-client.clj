#!/usr/bin/env bb
;; abantu-server-client.clj — bencode nREPL client for the abantu-server skill.
;;
;; Subcommands (first arg):
;;   load-and-start  load-file dev/dev.clj, then eval (server/start-server) in dev
;;   reload          eval (do (require 'dev) (dev/reload))   [== bb scripts/reload.clj]
;;   stop-server     eval (server/stop-server) in dev        [graceful, keeps nrepl]
;;   status          eval (server/running?) in dev, print result
;;
;; Reads `.nrepl-port` from cwd. Exits 0 on success, 1 on nREPL/eval error,
;; 2 on bad usage.
(ns abantu-server-client
  (:require [bencode.core :as b]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.net Socket]
           [java.io ByteArrayOutputStream File]))

(defn- port-file []
  (let [f (io/file ".nrepl-port")]
    (when (and (.exists f) (pos? (.length f))) f)))

(defn- read-port [^File f]
  (some-> (re-find #"\d+" (str/trim (slurp f))) parse-long))

(defn- ->str [x]
  (cond (nil? x) nil (string? x) x (bytes? x) (String. ^bytes x "UTF-8") :else (str x)))

(defn- send-msg! [out msg]
  (let [ba (ByteArrayOutputStream.)]
    (b/write-bencode ba msg)
    (.write out (.toByteArray ba)))
  (.flush out))

(defn- done? [m]
  (let [s (get m "status")]
    (and (sequential? s) (some #(= "done" (->str %)) s))))

(defn- read-resps [in]
  (let [rdr (java.io.PushbackInputStream. in)]
    (loop [resps []]
      (let [msg (try (b/read-bencode rdr)
                     (catch Exception e
                       (if (instance? java.io.EOFException e) ::eof (throw e))))]
        (cond (identical? ::eof msg) resps
              (nil? msg) resps
              (done? msg) (conj resps msg)
              :else (recur (conj resps msg)))))))

(defn- summarize [resps]
  (let [outs   (keep #(-> % (get "out") ->str) resps)
        errs   (keep #(-> % (get "err") ->str) resps)
        status (->> resps (keep #(get % "status")) (mapcat #(map ->str %)))
        ex     (some #(-> % (get "ex") ->str) resps)
        root-ex (some #(-> % (get "root-ex") ->str) resps)
        value  (some #(-> % (get "value") ->str) resps)
        err?   (or (some #{"eval-error" "syntax-error" "unknown-op" "load-error"} status)
                   (some? ex))]
    (when (seq outs) (print (str/join outs)))
    (when (seq errs) (binding [*out* *err*] (print (str/join errs))))
    (when err?
      (when ex      (binding [*out* *err*] (println "\n[abantu-server] eval error:" ex)))
      (when root-ex (binding [*out* *err*] (println "[abantu-server] root exception:" root-ex))))
    (flush)
    {:ok (not err?) :value value :status status}))

(defn- eval-op [in out code & [ns]]
  (send-msg! out (cond-> {"op" "eval" "code" code} ns (assoc "ns" ns)))
  (summarize (read-resps in)))

(defn- load-file-op [in out file-path file-name]
  (send-msg! out {"op" "load-file"
                  "file"      (slurp file-path)
                  "file-name" file-name
                  "file-path" file-path})
  (summarize (read-resps in)))

(defn -main [& args]
  (let [cmd (or (first args) "")
        pf  (port-file)]
    (cond
      (#{"" "-h" "--help"} cmd)
      (do (println "usage: abantu-server-client.clj <load-and-start|reload|stop-server|status>")
          (System/exit 0))

      (not (#{ "load-and-start" "reload" "stop-server" "status"} cmd))
      (do (binding [*out* *err*] (println "[abantu-server] unknown client command:" cmd))
          (System/exit 2))

      (nil? pf)
      (do (println "[abantu-server] no .nrepl-port in cwd; is nrepl running?")
          (System/exit 1))

      :else
      (let [port (read-port pf)]
        (if-not port
          (do (println "[abantu-server] could not parse port from .nrepl-port")
              (System/exit 1))
          (with-open [sock (Socket. "localhost" (int port))]
            (let [in  (.getInputStream sock)
                  out (.getOutputStream sock)]
              (case cmd
                "load-and-start"
                (let [r1 (load-file-op in out "dev/dev.clj" "dev.clj")]
                  (when-not (:ok r1) (System/exit 1))
                  (let [r2 (eval-op in out "(server/start-server)" "dev")]
                    (System/exit (if (:ok r2) 0 1))))

                "reload"
                (let [r (eval-op in out "(do (require 'dev) (dev/reload))")]
                  (System/exit (if (:ok r) 0 1)))

                "stop-server"
                (let [r (eval-op in out "(server/stop-server)" "dev")]
                  (System/exit (if (:ok r) 0 1)))

                "status"
                (let [r (eval-op in out "(server/running?)" "dev")]
                  (println "[abantu-server] running? =" (:value r))
                  (System/exit (if (:ok r) 0 1)))))))))))

(apply -main *command-line-args*)

