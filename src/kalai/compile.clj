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
            [clojure.java.shell :as sh]
            [camel-snake-kebab.core :as csk]
            [clojure.tools.analyzer.env :as env]
            [clojure.tools.reader :as reader])
  (:import (java.io File)
           (java.nio.file Path)
           (java.nio.file Paths)))

(def ext {::l/rust ".rs"
          ::l/cpp  ".cpp"
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

(defn analyze-forms [forms]
  (mapv az/analyze+eval forms))

(defn transpile-forms [forms]
  (-> (analyze-forms forms)
      (rewriters)))

(defn transpile-source-file-content [file-path & [out verbose lang]]
  (with-redefs [azu/ns-url ns-url]
    (-> (az/analyze-ns file-path)
        (rewriters))))

(defn javaize [^String filename]
  (let [i (.lastIndexOf filename ".")]
    (assert (pos? i) "must have an extension")
    (str (csk/->PascalCase (subs filename 0 i))
         (subs filename i))))

(defn write-target-file [content ^String relative-path out lang]
  (let [p (Paths/get relative-path (into-array String []))
        filename (javaize (str (.getFileName p)))
        package-name (str/lower-case (csk/->camelCase (str (.getParent p))))
        output-file (io/file out package-name (str/replace filename #"\.clj[csx]?$" (ext lang)))]
    (.mkdirs (io/file (.getParent output-file)))
    (spit output-file content)))

(defn relative [^File base ^File file]
  (.getPath (.relativize (.toURI base) (.toURI file))))

(defn transpile [{:keys [in out language]}]
  (let [base (io/file in)]
    (doseq [^File file (file-seq base)
            :when (not (.isDirectory file))
            :let [s (transpile-source-file-content file)
                  target (relative base file)]]
      (write-target-file s target out language))))

(defn compile-target-file [^File file-path language target]
  (println "Compiling" (str file-path))
  (.mkdirs (io/file target))
  (let [{:keys [exit out err]} (sh/sh "javac" "-d" target (str file-path))]
    (if (zero? exit)
      nil
      (do
        (println out)
        (println err)))))

(defn target-compile [{:keys [out language]}]
  (let [{:keys [exit out err]} (sh/sh "gradle" "build" :dir "examples")]
    (if (zero? exit)
      nil
      (str out \newline err))))
