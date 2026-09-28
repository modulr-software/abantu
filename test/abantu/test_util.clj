(ns abantu.test-util
  (:require [clojure.java.io :as io]
            [abantu.config :as conf]
            [abantu.db-test-utils :as db-test-utils]))

(defn- tmp-dir []
  (let [file (java.io.File/createTempFile "abantu-test-" "")]
    (.delete file)
    (.mkdir file)
    (.getAbsolutePath file)))

(defn with-test-db
  "Creates a fresh test DB isolated in a temp dir, runs `f` with the open
   connection, then deletes the temp dir."
  ([f] (with-test-db 1 f))
  ([db-id f]
   (let [tmp (tmp-dir)
         orig-read (var-get #'conf/read-value)]
     (with-redefs [conf/read-value (fn [& ks]
                                     (if (and (= (first ks) :database)
                                              (= (second ks) :dir))
                                       tmp
                                       (apply orig-read ks)))]
       (try
         (f (db-test-utils/create-test-db! db-id))
         (finally
           (when (.exists (io/file tmp))
             (io/delete-file (io/file tmp) true))))))))

(defn with-test-student-db
  "Creates a fresh master test DB and a migrated student test DB (student
   schema) isolated in a temp dir, runs `f` with the open student
   connection, then deletes the temp dir."
  ([f] (with-test-student-db 1 2 f))
  ([master-id student-id f]
   (let [tmp (tmp-dir)
         orig-read (var-get #'conf/read-value)]
     (with-redefs [conf/read-value (fn [& ks]
                                     (if (and (= (first ks) :database)
                                              (= (second ks) :dir))
                                       tmp
                                       (apply orig-read ks)))]
       (try
         (let [master-ds (db-test-utils/create-test-db! master-id)]
           (f (db-test-utils/create-test-student-db! student-id master-ds)))
         (finally
           (when (.exists (io/file tmp))
             (io/delete-file (io/file tmp) true))))))))