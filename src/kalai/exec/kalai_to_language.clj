(ns kalai.exec.kalai-to-language
  (:refer-clojure :exclude [compile])
  (:require [kalai.emit.langs :as l]
            [kalai.pass.kalai.pipeline :as kalai-pipeline]
            [kalai.pass.java.pipeline :as java-pipeline]
            [kalai.pass.rust.pipeline :as rust-pipeline]
            [kalai.pass.rust.e-string :as e-string]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.jvm.utils :as azu]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [camel-snake-kebab.core :as csk]
            [kalai.util :as u]
            [clojure.string :as string])
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

(defn stringify-rust-coll-helper-fns [forms]
  (->> forms
       (analyze-forms)
       (kalai-pipeline/asts->kalai)
       (rust-pipeline/kalai->rust)
       ;; remove the first line, which is "use crate::kalai;"
       (str/split-lines)
       (drop 5)
       (str/join \newline)))

;; TODO: can we remove this?  becuase we are using concrete types instead of the wrappers
;; (ex: std::vec::Vec<BValue> instead of Vector for {:t {:mvector [:any]}} )
(defn helper-fn-impl-strs []
  (let [
        primitives [:bool :byte :char :int :long :float :double :string]
        vector-contains-fns (for [t primitives]
                           (let [t-str (e-string/kalai-type->rust t)]
                             (list 'defn (symbol (str "contains-" t-str))
                                   ^{:t :bool} [(with-meta 'self {:ref true}),
                                                (with-meta 'x {:t t})]
                                   '(.contains self ^:ref (kalai.BValue/from x)))))
        set-contains-fns (for [t primitives]
                           (let [t-str (e-string/kalai-type->rust t)]
                             (list 'defn (symbol (str "contains-" t-str))
                                   ^{:t :bool} [(with-meta 'self {:ref true}),
                                                (with-meta 'x {:t t})]
                                   '(.contains self ^:ref (kalai.BValue/from x)))))
        set-insert-fns (for [t primitives]
                         (let [t-str (e-string/kalai-type->rust t)]
                           (list 'defn (symbol (str "insert-" t-str))
                                 ^{:t :bool} [(with-meta 'self {:ref true
                                                                :mut true}),
                                              (with-meta 'x {:t t})]
                                 (list '.insert 'self '(kalai.BValue/from x)))))
        map-insert-fns (for [k-t primitives
                             v-t primitives]
                         (let [k-t-str (e-string/kalai-type->rust k-t)
                               v-t-str (e-string/kalai-type->rust v-t)]
                           (list 'defn (symbol (str "insert-" k-t-str "-" v-t-str))
                                 ^{:t {:option [v-t]}} [(with-meta 'self {:ref true
                                                                          :mut true}),
                                                        (with-meta 'k {:t k-t})
                                                        (with-meta 'v {:t v-t})]
                                 (list '.map '(.insert self (kalai.BValue/from k) (kalai.BValue/from v))
                                       (str "RUST-FROM-FN-" v-t-str)))))
        map-get-fns (for [k-t primitives
                          v-t primitives]
                      (let [k-t-str (e-string/kalai-type->rust k-t)
                            v-t-str (e-string/kalai-type->rust v-t)]
                        (list 'defn (symbol (str "get-" k-t-str "-" v-t-str))
                              ^{:t {:option [^:ref v-t]}} [(with-meta 'self {:ref true}),
                                                           (with-meta 'k {:t k-t
                                                                          :ref true})]
                              (list '.map '(.get self ^:ref (kalai.BValue/from k))
                                    (str "RUST-FROM-FN-" v-t-str)))))
        helper-fn-front-matter '((ns kalai.BValue)
                                 (defn from [x]))
        result-str (str/join \newline
                             ["impl Set {"
                              (stringify-rust-coll-helper-fns (concat helper-fn-front-matter
                                                                      set-contains-fns
                                                                      set-insert-fns))
                              "}"
                              "impl Map {"
                              (stringify-rust-coll-helper-fns (concat helper-fn-front-matter
                                                                      map-insert-fns
                                                                      map-get-fns))
                              "}"
                              "impl Vector {"
                              (stringify-rust-coll-helper-fns (concat helper-fn-front-matter
                                                                      vector-contains-fns))
                              "}"])
        ;; Note: because we cannot generate `::from` in Clojure, at least not easily, we use a placeholder string
        ;; in our above auto-generated methods as a workaround, and here is the other part of the workaround.
        result-str-post-edits (str/replace result-str #"String::from\(\"RUST-FROM-FN-(\w+)\"\)" "$1::from")]
    result-str-post-edits))


;; Note: if reosurce files are stored in a depth below the root directory, then
;; a deeper copy using recursion would be necessary, which would be better handled using the fs library
;; https://github.com/clj-commons/fs
(defn inject-kalai-helper-files [{:keys [transpile-dir languages]}]
  (when (contains? languages ::l/rust)
    (let [k (io/resource "rust/kalai.rs")
          dest-file-path (io/file transpile-dir "rust" "src" "kalai.rs")
          k-str (slurp k)
          dest-file-str (str k-str
                              \newline \newline
                             (helper-fn-impl-strs) ;; TODO: can we remove this?
                             \newline
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

(defn clj-file? [source-file]
  (and (not (.isDirectory source-file))
       (or (string/ends-with? (.getName source-file) ".clj")
           (string/ends-with? (.getName source-file) ".cljc"))))

(defn clj-files-in-dir [^String dir]
  (filter clj-file? (file-seq (io/file dir))))

(defn transpile-all
  "options is a map of
  {:src-dir \"src\"           ;; a directory containing Kalai source files that are inputs to transpilation>
   :transpile-dir \"src/main\"         ;; a the root directory for target language transpiled output>
   :languages #{:kalai.emit.lang/java} ;; the desired target languages
   }"
  ;; TODO: consider adding a spec to this
  [options]
  (doseq [^File source-file (clj-files-in-dir (:src-dir options))]
    (transpile-file source-file options))
  (inject-kalai-helper-files options)
  (write-module-definitions options))
