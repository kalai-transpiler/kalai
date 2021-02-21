(ns kalai.pass.test-helpers
  (:require [clojure.test :refer [testing is]]
    ;; Loading placation enables it to provide better string diffs,
    ;; however not all tooling plays nicely with it
    ;;[kalai.placation]
            [kalai.kalai-to-language :as t]
            [kalai.pass.kalai.pipeline :as kalai-pipeline]
            [kalai.pass.java.pipeline :as java-pipeline]
            [kalai.pass.rust.pipeline :as rust-pipeline]
            [kalai.util :as u]
            [clojure.string :as str]))

(defn as-ns [form]
  (list '(ns test-package.test-class)
        form))

(defn as-function [form]
  (list '(ns test-package.test-class)
        (list 'defn 'test-function ^{:t :void} [] form)))

(defn remove-kalai-class [s]
  (nth s 2))

(defn remove-java-class [s]
  (->> s
       (str/split-lines)
       (drop 6)
       (butlast)
       (str/join \newline)))

(defn remove-kalai-function [s]
  (nth (remove-kalai-class s) 3))

(defn remove-java-function [s]
  (->> s
       (str/split-lines)
       (drop 7)
       (drop-last 2)
       (str/join \newline)))

;; TODO: refactor target-language as an argument to this test fn, and
;; create (a) map(s) with each target language's pipline fn, display name, etc.
(defn test-form [input kalai-s-expression expected as remove-kalai remove-java target-lang-pipeline]
  `(do
     (reset! u/c 0)
     (is ;; to capture expections for test reporting
       (let [asts# (t/analyze-forms (~as ~input))
             a2b# (kalai-pipeline/asts->kalai asts#)]
         (and
           (testing "compiling to kalai"
             (or (is (= ~kalai-s-expression (~remove-kalai a2b#)))
                 (println "Clojure to Kalai failed")))
           (let [b2c# (~target-lang-pipeline a2b#)]
             (and
               (testing "compiling kalai to target"
                 (or
                   (is (= ~expected (~remove-java b2c#)))
                   (println "Kalai to target failed")))
               (reset! u/c 0))))))))

(defmacro ns-form [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
             identity identity identity java-pipeline/kalai->java))

(defmacro top-level-form [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
             as-ns remove-kalai-class remove-java-class java-pipeline/kalai->java))

(defmacro inner-form [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
             as-function remove-kalai-function remove-java-function java-pipeline/kalai->java))


(defn remove-rust-class [s]
  (->> s
    (str/split-lines)
    (drop 7)
    (str/join \newline)))

(defn remove-rust-function [s]
  (->> s
    (str/split-lines)
    (drop 8)
    (butlast)
    (str/join \newline)))

(defmacro ns-form-rust [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
    identity identity identity rust-pipeline/kalai->rust))

(defmacro top-level-form-rust [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
    as-ns remove-kalai-class remove-rust-class rust-pipeline/kalai->rust))

(defmacro inner-form-rust [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
    as-function remove-kalai-function remove-rust-function rust-pipeline/kalai->rust))
