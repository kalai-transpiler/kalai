(ns kalai.compile
  (:refer-clojure :exclude [compile])
  (:require [kalai.emit.util :as emit.util]
            [kalai.emit.langs :as l]
            [kalai.pass.kalai.pipeline :as kalai-pipeline]
            [kalai.pass.java.pipeline :as java-pipeline]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.jvm.utils :as azu]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [puget.printer :as puget]
            [clojure.java.shell :as sh])
  (:import (java.io File)))

(def ext {::l/rust ".rs"
          ::l/cpp ".cpp"
          ::l/java ".java"})

(defn ns-url [file-path]
  (io/as-url (io/file file-path)))

;; Initial implementation

(defn compile-file [file-path out verbose lang]
  (with-redefs [azu/ns-url ns-url]
    (let [asts (az/analyze-ns file-path)
          strs (emit.util/emit-analyzed-ns-asts asts lang)
          output-file (io/file (str out "/"
                                    (str/replace file-path #"\.clj[csx]?$" "")
                                    (ext lang)))]
      (.mkdirs (io/file (.getParent output-file)))
      (spit output-file (str/join \newline strs)))))


;; Proposed rewriting s-expressions pipeline implementation

(defn rewriters [asts]
  (->> (kalai-pipeline/asts->kalai asts)
       (java-pipeline/kalai->java)))

(defn compile-forms [forms]
  (-> (map az/analyze forms)
      (rewriters)))

(defn compile-source-file [file-path & [out verbose lang]]
  (with-redefs [azu/ns-url ns-url]
    (-> (az/analyze-ns file-path)
        (rewriters))))

(defn write-target-file [s file-path out lang]
  (let [output-file (io/file (str out "/" (str/replace file-path #"\.clj[csx]?$" (ext lang))))]
    (.mkdirs (io/file (.getParent output-file)))
    (spit output-file s)))

(defn relative [^File base ^File file]
  (.getPath (.relativize (.toURI base) (.toURI file))))

(defn compile [{:keys [in out language]}]
  (let [base (io/file in)]
    (doseq [^File file (file-seq base)
            :when (not (.isDirectory file))
            :let [s (compile-source-file file)
                  target (relative base file)]]
      (write-target-file s target out language))))

(defn compile-target-file [file-path language target]
  (println "Compiling" (str file-path))
  (let [{:keys [exit out err]} (sh/sh "javac" "-d" target (str file-path))]
    (if (zero? exit)
      nil
      (do
        (println out)
        (println err)))))

(defn target-compile [{:keys [out language target]}]
  (doseq [^File file (file-seq (io/file out))
          :when (not (.isDirectory file))]
    (compile-target-file file language target)))
