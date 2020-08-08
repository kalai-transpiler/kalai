(ns kalai.unconstrianed-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.compile :as c]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]
            [kalai.placation :as p]
            [clojure.string :as str]
            [meander.epsilon :as m]))

(defn as-ns [form]
  (list
    '(ns test-package.test-class)
    form))

(defn as-function [form]
  (list
    '(ns test-package.test-class)
    (list 'defn 'test-function [] form)))

(defn remove-class [s]
  (->> s
       (str/split-lines)
       (drop 2)
       (butlast)
       (str/join \newline)))

(defn remove-function [s]
  (->> s
       (str/split-lines)
       (drop 3)
       (drop-last 2)
       (str/join \newline)))

;; this is a macro to get line number for free
(defmacro assert-top-level-form [{:keys [input expected]}]
  `(->> (as-ns ~input)
        (c/compile-forms)
        (remove-class)
        (p/is= ~expected)))

(defmacro assert-inner-form [{:keys [input expected]}]
  `(->> (as-function ~input)
        (c/compile-forms)
        (remove-function)
        (p/is= ~expected)))


;; maybe we want to test the compiler on inner forms

(deftest bindings-def
  (assert-top-level-form
    {:input    '(def ^{:t "int"} x 3)
     :expected "int x = 3;"}))

(deftest bindings-def2
  (assert-inner-form
    {:input    '(do (def ^{:t "Boolean"} x true)
                    (def ^{:t "Long"} y 5))
     :expected "Boolean x = true;
Long y = 5;"}))
