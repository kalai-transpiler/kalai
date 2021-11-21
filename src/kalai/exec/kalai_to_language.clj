(ns kalai.exec.kalai-to-language
  (:refer-clojure :exclude [compile])
  (:require [kalai.emit.langs :as l]
            [kalai.pass.kalai.pipeline :as kalai-pipeline]
            [kalai.pass.java.pipeline :as java-pipeline]
            [kalai.pass.rust.pipeline :as rust-pipeline]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.jvm.utils :as azu]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [camel-snake-kebab.core :as csk]
            [kalai.util :as u])
  (:import (java.io File)
           (java.nio.file Paths)))

(def ext {::l/rust ".rs"
          ::l/cpp  ".cpp"
          ::l/java ".java"})

(def translators
  {::l/java java-pipeline/kalai->java
   ::l/rust rust-pipeline/kalai->rust})

(def file-naming-conventions
  {::l/java (fn java-file-naming [^String filename]
              (let [i (.lastIndexOf filename ".")]
                (assert (pos? i) "must have an extension")
                (str (csk/->PascalCase (subs filename 0 i))
                     (subs filename i))))
   ::l/rust (fn rust-file-naming [^String filename]
              (-> filename
                  (csk/->snake_case)
                  (str/lower-case)))})

(def package-naming-conventions
  {::l/java (fn [x] (-> x
                        (csk/->camelCase)
                        (str/lower-case)))
   ::l/rust (fn [x] (-> x
                        (csk/->snake_case)
                        (str/lower-case)))})

(defn analyze-forms [forms]
  (mapv az/analyze+eval forms))

(defn ns-url [file-path]
  (io/as-url (io/file file-path)))

(defn analyze-file [file-path]
  (with-redefs [azu/ns-url ns-url]
    (az/analyze-ns file-path)))

(defn read-kalai [file]
  (-> (analyze-file file)
      (kalai-pipeline/asts->kalai)))

(defn relative [^File base ^File file]
  (.getPath (.relativize (.toURI base) (.toURI file))))

(defn write-file [^String content ^String relative-path ^File transpile-dir lang]
  (reset! u/c 0)
  (let [file-naming (get file-naming-conventions lang)
        ;; "src" might be "test" sometimes, and might be language specific
        path (Paths/get (name lang) (into-array String ["src" relative-path]))
        target (-> (.getFileName path)
                   (str)
                   (file-naming)
                   (str/replace #"\.clj[csx]?$" (ext lang)))
        package-naming (get package-naming-conventions lang)
        package-name (-> (str (.getParent path))
                         (package-naming))
        output-file (io/file transpile-dir package-name target)]
    (.mkdirs (io/file (.getParent output-file)))
    (spit output-file content)))

(defn transpile-file [^File source-file {:keys [src-dir transpile-dir languages verbose]}]
  (when verbose
    (println "transpiling source file:" (str source-file)))
  (let [kalai (read-kalai source-file)
        relative-path (relative (io/file src-dir) source-file)]
    (doseq [[language translate] (select-keys translators languages)]
      (-> (translate kalai)
          (write-file relative-path transpile-dir language)))))

(defn write-module-definition [dir]
  (spit (io/file dir "mod.rs")
        (str/join (->> (.listFiles dir)
                       (map (memfn ^File getName))
                       (map #(str/replace % #".rs$" ""))
                       (remove #{"mod" "lib"})
                       (sort)
                       (map #(str "pub mod " % ";\n"))))))

(defn f []
  (let [set-helper-fn
        '((ns BValue)
          (defn from [x])
          (defn contains-f32 ^{:t :bool} [^{:ref true} self, ^{:t :float} x]
            (.contains self ^:ref (BValue/from x))))
        set-helper-fn-str
        (->> set-helper-fn
             (analyze-forms)
             (kalai-pipeline/asts->kalai)
             (rust-pipeline/kalai->rust)
             ;; remove the first line, which is "use crate::kalai;"
             (str/split-lines)
             (drop 4)
             (str/join \newline))]
    (str/join \newline ["impl Set {"
                        set-helper-fn-str
                        "}"])))


;; Note: if reosurce files are stored in a depth below the root directory, then
;; a deeper copy using recursion would be necessary, which would be better handled using the fs library
;; https://github.com/clj-commons/fs
(defn inject-kalai-helper-files [{:keys [transpile-dir languages]}]
  (when (contains? languages ::l/rust)
    (let [k (io/resource "rust/kalai.rs")
          dest-file-path (io/file transpile-dir "rust" "src" "kalai.rs")
          k-str (slurp k)
          dest-file-str (str k-str
                             ;; TODO: Decide if we want to append the contents to kalai.rs or have a different file
                             ;; TODO: Fill out all helper methods for all wrapper types that we need, not just 1 example for Set
                             ;; TODO: Replace current kalai.rs (Value enum) with traitobjs.rs (Value trait) <-> updating Clojure Rust pipeline code (ex: around collections)
                             ;; \newline \newline
                             ;;(f)
                             ;;\newline
                             )]
      (assert k "kalai.rs")
      (spit dest-file-path dest-file-str))))

(defn write-module-definitions [{:keys [transpile-dir languages]}]
  (when (contains? languages ::l/rust)
    (let [src (io/file transpile-dir (name ::l/rust) "src")]
      (doseq [dir (file-seq src)
              :when (.isDirectory dir)]
        (write-module-definition dir))
      (.renameTo (io/file src "mod.rs") (io/file src "lib.rs")))))

(defn transpile-all
  "options is a map of
  {:src-dir \"src\"           ;; a directory containing Kalai source files that are inputs to transpilation>
   :transpile-dir \"src/main\"         ;; a the root directory for target language transpiled output>
   :languages #{:kalai.emit.lang/java} ;; the desired target languages
   }"
  ;; TODO: consider adding a spec to this
  [options]
  (doseq [^File source-file (file-seq (io/file (:src-dir options)))
          :when (not (.isDirectory source-file))]
    (transpile-file source-file options))
  (inject-kalai-helper-files options)
  (write-module-definitions options))
