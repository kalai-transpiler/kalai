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
  (let [output-file (io/file (str out "/" (str/replace file-path #"\.clj[csx]?$" "") (ext lang)))]
    (.mkdirs (io/file (.getParent output-file)))
    (spit output-file s)))

(defn compile [{:keys [in out language]}]
  (doseq [^File file (file-seq (io/file in))
          :when (not (.isDirectory file))
          :let [s (compile-source-file (str file))]]
    (write-target-file s (str file) out language)))
