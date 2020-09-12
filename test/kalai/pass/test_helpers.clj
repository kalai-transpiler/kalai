(ns kalai.pass.test-helpers
  (:require [clojure.test :refer :all]
            [kalai.compile :as c]
            [kalai.pass.kalai.pipeline :as kalai-pipeline]
            [kalai.pass.java.pipeline :as java-pipeline]
            [kalai.pass.java.a-syntax :as a-syntax]
            #_[kalai.placation]
            [clojure.string :as str]
            [clojure.tools.analyzer.jvm :as az]))

(defn as-ns [form]
  (list '(ns test-package.test-class)
        form))

(defn as-function [form]
  (list '(ns test-package.test-class)
        (list 'defn 'test-function [] form nil)))

(defn remove-kalai-class [s]
  (nth s 2))

(defn remove-java-class [s]
  (->> s
       (str/split-lines)
       (drop 2)
       (butlast)
       (str/join \newline)))

(defn remove-kalai-function [s]
  (second (nth (remove-kalai-class s) 5)))

(defn remove-java-function [s]
  (->> s
       (str/split-lines)
       (drop 3)
       (drop-last 3)
       (str/join \newline)))

(defn test-form [input kalai-s-expression expected as remove-kalai remove-java]
  `(do
     (reset! a-syntax/c 0)
     (is ;; to capture expections for test reporting
       (let [asts# (map az/analyze (~as ~input))
             a2b# (kalai-pipeline/asts->kalai asts#)]
         (and
           (testing "compiling to kalai"
             (or (is (= ~kalai-s-expression (~remove-kalai a2b#)))
                 (println "Clojure to Kalai failed")))
           (reset! a-syntax/c 0)
           (let [b2c# (java-pipeline/kalai->java a2b#)]
             (and
               (testing "compiling kalai to java"
                 (or
                   (is (= ~expected (~remove-java b2c#)))
                   (println "Kalai to Java failed")))
               (reset! a-syntax/c 0)
               (let [a2c# (c/compile-forms (~as ~input))]
                 (testing "compiling to java"
                   (or (is (= a2c# b2c#))
                       (println "Clojure to Java failed")))))))))))

(defmacro top-level-form [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
             as-ns remove-kalai-class remove-java-class))

(defmacro inner-form [input kalai-s-expression expected]
  (test-form input kalai-s-expression expected
             as-function remove-kalai-function remove-java-function))
