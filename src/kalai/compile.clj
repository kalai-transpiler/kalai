(ns kalai.compile
  (:refer-clojure :exclude [compile])
  (:require [kalai.emit.util :as emit.util]
            [kalai.emit.langs :as l]
            [kalai.pass.a-annotate-ast :as a-analyze]
            [kalai.pass.b-kalai-constructs :as b-kalai-ast]
            [kalai.pass.c-annotate-return :as c-annotate-return]
            [kalai.pass.d1-java-syntax :as d1-java-syntax]
            [kalai.pass.d2-java-syslib :as d2-java-syslib]
            [kalai.pass.d3-java-condense :as d3-java-condense]
            [kalai.pass.d4-java-string :as d4-java-string]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.jvm.utils :as azu]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]
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

(defn rewriters [ast]
  (-> ast
      (a-analyze/rewrite)
      (e/emit-form)
      (b-kalai-ast/rewrite)
      (c-annotate-return/rewrite)
      (d1-java-syntax/rewrite)
      (d2-java-syslib/rewrite)
      (d3-java-condense/rewrite)
      ;;(doto (prn 'SPY))
      (d4-java-string/stringify-entry)))

(defn compile-source-file [file-path & [out verbose lang]]
  (with-redefs [azu/ns-url ns-url]
    (-> (az/analyze-ns file-path)
        (rewriters))))

(defn write-target-file [s file-path out lang]
  (let [output-file (io/file (str out "/" (str/replace file-path #"\.clj[csx]?$" "") (ext lang)))]
    (.mkdirs (io/file (.getParent output-file)))
    (spit output-file s)))

(defn compile [{:keys [in out language]}]
  (doseq [^File file (file-seq (io/file in))
          :when (not (.isDirectory file))
          :let [s (compile-source-file (str file))]]
    (write-target-file s (str file) out language)))
