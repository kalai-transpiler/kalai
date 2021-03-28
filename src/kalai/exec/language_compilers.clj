(ns kalai.exec.language-compilers
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [kalai.emit.langs :as l])
  (:import (java.io File)))

(defn compile-target-file [^File file-path language out-dir]
  (println "Compiling" (str file-path))
  (.mkdirs (io/file out-dir))
  (let [{:keys [exit out err]}
        (case language
          ::l/rust (sh/sh "rustc" "--out-dir" out-dir (str file-path))
          ::l/java (sh/sh "javac" "-d" out-dir (str file-path)))]
    (if (zero? exit)
      nil
      (do
        (println out)
        (println err)))))

(defn cargo-build [dir]
  (let [{:keys [exit out err]} (sh/sh "cargo" "build" :dir dir)]
    (if (zero? exit)
      nil
      (str out \newline err))))

(defn gradle-build [dir]
  (let [{:keys [exit out err]} (sh/sh "gradle" "build" :dir dir)]
    (if (zero? exit)
      nil
      (str out \newline err))))

(defn build [languages dir]
  ;; TODO: we might want to print something out and return error code if it
  ;; fails.
  ;; TODO: consider exiting early when one of the individual language targets
  ;; fails.
  (when (contains? languages ::l/rust)
    (cargo-build (io/file dir "rust")))
  (when (contains? languages ::l/java)
    (gradle-build (io/file dir "java"))))
