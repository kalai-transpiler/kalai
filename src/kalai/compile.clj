(ns kalai.compile
  (:refer-clojure :exclude [compile])
  (:require [kalai.emit.util :as emit.util]
            [kalai.emit.langs :as l]
            [kalai.pass.a-annotate-ast :as a-annotate-ast]
            [kalai.pass.b-kalai-constructs :as b-kalai-constructs]
            [kalai.pass.c-flatten-groups :as c-flatten-groups]
            [kalai.pass.d-annotate-return :as d-annotate-return]
            [kalai.pass.java1-syntax :as java1-syntax]
            [kalai.pass.java2-syslib :as java2-syslib]
            [kalai.pass.java3-condense :as java3-condense]
            [kalai.pass.java4-string :as java4-string]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.jvm.utils :as azu]
            [clojure.tools.analyzer.passes.jvm.emit-form :as azef]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [puget.printer :as puget])
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

(defn spy [x]
  (doto x puget/cprint))

(defn rewriters [asts]
  (->> asts
       (map a-annotate-ast/rewrite)
       (map azef/emit-form)
       (b-kalai-constructs/rewrite)
       (c-flatten-groups/rewrite)
       (d-annotate-return/rewrite)
       (c-flatten-groups/rewrite) ;; repeat because returns can create groups
       (spy)

       (java1-syntax/rewrite)
       (java2-syslib/rewrite)
       (java3-condense/rewrite)
       ;;(spy)
       (java4-string/stringify-entry)))

;;(e/emit-form (az/analyze '(defn test-function [] (do (def x true) (def y 5)))))

(defn compile-forms [forms]
  (-> (map az/analyze forms)
      (rewriters)))

(defn compile-source-file [file-path & [out verbose lang]]
  (with-redefs [azu/ns-url ns-url]
    (-> (az/analyze-ns file-path)
        ;; probably need (->> (map rewriters)) here
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
