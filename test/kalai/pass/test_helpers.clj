(ns kalai.pass.test-helpers
  (:require [clojure.test :refer [testing is]]
    ;; Loading placation enables it to provide better string diffs,
    ;; however not all tooling plays nicely with it
    ;;[kalai.placation]
            [kalai.compile :as c]
            [kalai.pass.kalai.pipeline :as kalai-pipeline]
            [kalai.pass.java.pipeline :as java-pipeline]
            [kalai.util :as u]
            [clojure.string :as str]
            [clojure.tools.analyzer.jvm :as az]))

(defn as-ns [form]
  (list '(ns test-package.test-class)
        form))

(defn as-function [form]
  (list '(ns test-package.test-class)
        (list 'defn 'test-function ^{:t "void"} [] form)))

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

(defn test-form [input kalai-s-expression expected as remove-kalai remove-java]
  `(do
     (reset! u/c 0)
     (is ;; to capture expections for test reporting
       (let [asts# (map az/analyze (~as ~input))
             a2b# (kalai-pipeline/asts->kalai asts#)]
         (and
           (testing "compiling to kalai"
             (or (is (= ~kalai-s-expression (~remove-kalai a2b#)))
                 (println "Clojure to Kalai failed")))
           (let [b2c# (java-pipeline/kalai->java a2b#)]
             (and
               (testing "compiling kalai to java"
                 (or
                   (is (= ~expected (~remove-java b2c#)))
                   (println "Kalai to Java failed")))
               (reset! u/c 0)
               (let [a2c# (c/compile-forms (~as ~input))]
                 (testing "compiling to java"
                   (or (is (= a2c# b2c#))
                       (println "Clojure to Java failed")))))))))))

(defmacro ns-form [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
             identity identity identity))

(defmacro top-level-form [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
             as-ns remove-kalai-class remove-java-class))

(defmacro inner-form [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
             as-function remove-kalai-function remove-java-function))
