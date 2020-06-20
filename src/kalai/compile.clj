(ns kalai.compile
  (:refer-clojure :exclude [compile])
  (:require [kalai.emit.util :as emit.util]
            [kalai.emit.langs :as l]
            [kalai.pass.analyze :as analyze]
            [kalai.pass.ast-patterns :as ast-patterns]
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

;; NEXT TIME:
; * make test case!
; ** (compile-file-new "examples/a/demo01.clj")
; * maybe more concepts? maybe more java out?

(defn compile-file-new [file-path & [out verbose lang]]
  (with-redefs [azu/ns-url ns-url]
    (-> file-path
        (analyze/analyze)
        (ast-patterns/namespace-forms)
        ;;(java-ast/java-ast)
        ;;(java-condense/java-condense)
        ;;(java-string/java-string)
        )))


(defn compile [{:keys [in out verbose language]}]
  (doseq [^File file (file-seq (io/file in))
          :when (not (.isDirectory file))]
    (compile-file (str file) out verbose language)))
