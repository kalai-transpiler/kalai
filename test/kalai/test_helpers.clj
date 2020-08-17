(ns kalai.test-helpers
  (:require [clojure.test :refer :all]
            [kalai.compile :as c]
    ;;[kalai.placation]
            [clojure.string :as str]))

(defn as-ns [form]
  (list '(ns test-package.test-class)
        form))

(defn as-function [form]
  (list '(ns test-package.test-class)
        (list 'defn 'test-function [] form nil)))

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
       (drop-last 3)
       (str/join \newline)))

(defmacro top-level-form [input expected]
  `(is (= ~expected
          (remove-class (c/compile-forms (as-ns ~input))))))

(defmacro inner-form [input expected]
  `(is (= ~expected
          (remove-function (c/compile-forms (as-function ~input))))))
