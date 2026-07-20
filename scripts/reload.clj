#!/usr/bin/env bb
(ns reload
  (:require [bencode.core :as b]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.net Socket]
           [java.io ByteArrayOutputStream]))

(defn- port-file []
  (let [f (io/file ".nrepl-port")]
    (when (and (.exists f)
               (pos? (.length f)))
      f)))

(defn- read-port [^java.io.File f]
  (try
    (let [s (str/trim (slurp f))]
      (some-> (re-find #"\d+" s) parse-long))
    (catch Exception _ nil)))

(defn- send-msg! [^java.io.OutputStream out msg]
  (let [ba (ByteArrayOutputStream.)]
    (b/write-bencode ba msg)
    (.write out (.toByteArray ba)))
  (.flush out))

(defn- ->str [x]
  (cond
    (nil? x)     nil
    (string? x) x
    (bytes? x)  (String. ^bytes x "UTF-8")
    :else        (str x)))

(defn- done-status? [m]
  (let [s (get m "status")]
    (and (sequential? s)
         (some #(= "done" (->str %)) s))))

(defn- read-resps [^java.io.InputStream in]
  (let [rdr (java.io.PushbackInputStream. in)]
    (loop [resps []]
      (let [msg (try (b/read-bencode rdr)
                     (catch Exception e
                       (if (instance? java.io.EOFException e) ::eof (throw e))))]
        (cond
          (identical? ::eof msg) resps
          (nil? msg) resps
          (done-status? msg) (conj resps msg)
          :else (recur (conj resps msg)))))))

(defn -main [& _args]
  (let [pf (port-file)]
    (if-not pf
      (do
        (println "[reload] no .nrepl-port in cwd; is ./nrepl.sh running?")
        (System/exit 1))
      (let [port (read-port pf)]
        (if-not port
          (do
            (println "[reload] could not parse port from .nrepl-port")
            (System/exit 1))
          (with-open [sock (Socket. "localhost" (int port))]
            (let [in  (.getInputStream sock)
                  out (.getOutputStream sock)]
              (send-msg! out {"op"   "eval"
                              "code" "(do (require 'dev) (dev/reload))"})
              (let [resps      (read-resps in)
                    outs       (keep #(-> % (get "out") ->str) resps)
                    errs       (keep #(-> % (get "err") ->str) resps)
                    status     (->> resps (keep #(get % "status")) (mapcat (fn [s] (map ->str s))))
                    ex         (some #(-> % (get "ex") ->str) resps)
                    root-ex    (some #(-> % (get "root-ex") ->str) resps)
                    err?       (or (some #{"eval-error" "syntax-error" "unknown-op"} status)
                                   (some? ex))]
                (when (seq outs)
                  (print (str/join outs)))
                (when (seq errs)
                  (binding [*out* *err*]
                    (print (str/join errs))))
                (when err?
                  (when ex
                    (binding [*out* *err*]
                      (println "\n[reload] eval error:" ex)))
                  (when root-ex
                    (binding [*out* *err*]
                      (println "[reload] root exception:" root-ex))))
                (flush)
                (System/exit (if err? 1 0))))))))))

(-main)
