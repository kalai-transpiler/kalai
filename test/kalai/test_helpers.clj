(ns kalai.test-helpers
  (:require [clojure.test :refer :all]
            [kalai.compile :as c]
    ;;[kalai.placation]
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

(defmacro top-level-form [input kalai-s-expression expected]
  `(let [asts# (map az/analyze (as-ns ~input))
         a2b# (c/asts->kalai asts#)
         b2c# (c/kalai->java a2b#)
         a2c# (c/compile-forms (as-ns ~input))]
     (and
       (testing "compiling to kalai"
         (is (= ~kalai-s-expression (remove-kalai-class a2b#))))
       (testing "compiling kalai to java"
         (is (= ~expected (remove-java-class b2c#))))
       (testing "compiling to java"
         (is (= a2c# b2c#))))))

(defmacro inner-form [input kalai-s-expression expected]
  `(let [asts# (map az/analyze (as-function ~input))
         a2b# (c/asts->kalai asts#)
         b2c# (c/kalai->java a2b#)
         a2c# (c/compile-forms (as-function ~input))]
     (and
       (testing "compiling to kalai"
         (is (= ~kalai-s-expression (remove-kalai-function a2b#))))
       (testing "compiling kalai to java"
         (is (= ~expected (remove-java-function b2c#))))
       (testing "compiling to java"
         (is (= a2c# b2c#))))))
