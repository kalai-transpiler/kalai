(ns kalai.compile
  (:refer-clojure :exclude [compile])
  (:require [kalai.emit.util :as emit.util]
            [kalai.emit.langs :as l]
            [kalai.pass.analyze :as analyze]
            [kalai.pass.ast-patterns :as ast-patterns]
            [kalai.pass.java-ast :as java-ast]
            [kalai.pass.java-condense :as java-condense]
            [kalai.pass.java-string :as java-string]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.jvm.utils :as azu]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import (java.io File)))

(def ext {::l/rust ".rs"
          ::l/cpp ".cpp"
          ::l/java ".java"})

(defn ns-url [file-path]
  (io/as-url (io/file file-path)))

(defn compile-file [file-path out verbose lang]
  (with-redefs [azu/ns-url ns-url]
    (let [asts (az/analyze-ns file-path)
          strs (emit.util/emit-analyzed-ns-asts asts lang)
          output-file (io/file (str out "/" (str/replace file-path #"\.clj[csx]?$" "") (ext lang)))]
      (.mkdirs (io/file (.getParent output-file)))
      (spit output-file (str/join \newline strs)))))

(defn compile-source-file [file-path & [out verbose lang]]
  (with-redefs [azu/ns-url ns-url]
    (-> file-path
        (analyze/analyze)
        (ast-patterns/namespace-forms)
        (java-ast/java-class)
        (java-condense/condense)
        (java-string/stringify))))

(defn write-target-file [s file-path out lang]
  (let [output-file (io/file (str out "/" (str/replace file-path #"\.clj[csx]?$" "") (ext lang)))]
    (.mkdirs (io/file (.getParent output-file)))
    (spit output-file s)))

(defn compile [{:keys [in out language]}]
  (doseq [^File file (file-seq (io/file in))
          :when (not (.isDirectory file))
          :let [s (compile-source-file (str file))]]
    (write-target-file s (str file) out language)))
