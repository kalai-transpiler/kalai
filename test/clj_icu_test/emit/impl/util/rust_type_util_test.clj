(ns clj-icu-test.emit.impl.util.rust-type-util-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.impl.util.rust-type-util :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
  (:import clj_icu_test.common.AstOpts))

(reset-indent-level)

(defexpect scalar
  (let [ast (az/analyze "pixel")]
    (expect "String::from(\"pixel\")"
            (emit (map->AstOpts {:ast ast :lang ::l/rust})))))

(defexpect vectors
  (do
    (import 'java.util.List)
    (let [ast (az/analyze '(def ^{:mtype [List [Integer]]} numbers [13 17 19 23]))]
      (expect "let numbers: Vec<i32> = vec![13, 17, 19, 23];"
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))
    (let [ast (az/analyze '(do (def x 13) (def ^{:mtype [List [Integer]]} numbers [x])))]
      (expect ["let x = 13;"
               "let numbers: Vec<i32> = vec![x];"]
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))
    (let [ast (az/analyze '(def ^{:mtype [List [List [Integer]]]} matrix [[2 3 5]
                                                                          [7 11 13]
                                                                          [17 19 23]]))]
      (expect "let matrixV0: Vec<i32> = vec![2, 3, 5];
let matrixV1: Vec<i32> = vec![7, 11, 13];
let matrixV2: Vec<i32> = vec![17, 19, 23];
let matrix: Vec<Vec<i32>> = vec![matrixV0, matrixV1, matrixV2];"
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))))

(defexpect maps
  (do
    (import 'java.util.Map)
    (let [ast (az/analyze '(def ^{:mtype [Map [String Integer]]} numberWords {"one" 1
                                                                              "two" 2
                                                                              "three" 3}))]
      (expect "let mut numberWords: HashMap<String,i32> = HashMap::new();
numberWords.insert(String::from(\"one\"), 1);
numberWords.insert(String::from(\"two\"), 2);
numberWords.insert(String::from(\"three\"), 3);"
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))))
