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
      (expect
"lazy_static! {
  static ref numbers: Vec<i32> = vec![13, 17, 19, 23];
}"
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))
    (let [ast (az/analyze '(do (def x 13) (def ^{:mtype [List [Integer]]} numbers [x])))]
      (expect
["lazy_static! {
  static ref x = 13;
}"
"lazy_static! {
  static ref numbers: Vec<i32> = vec![x];
}"]
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))
    (let [ast (az/analyze '(def ^{:mtype [List [List [Integer]]]} matrix [[2 3 5]
                                                                          [7 11 13]
                                                                          [17 19 23]]))]
      (expect
"lazy_static! {
  static ref matrixV0: Vec<i32> = vec![2, 3, 5];
  static ref matrixV1: Vec<i32> = vec![7, 11, 13];
  static ref matrixV2: Vec<i32> = vec![17, 19, 23];
  static ref matrix: Vec<Vec<i32>> = vec![matrixV0, matrixV1, matrixV2];
}"
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))))

(defexpect maps
  (do
    (import 'java.util.Map)
    (let [ast (az/analyze '(def ^{:mtype [Map [String Integer]]} numberWords {"one" 1
                                                                              "two" 2
                                                                              "three" 3}))]
      (expect
"lazy_static! {
  static ref mut numberWords: HashMap<String,i32> = HashMap::new();
  numberWords.insert(String::from(\"one\"), 1);
  numberWords.insert(String::from(\"two\"), 2);
  numberWords.insert(String::from(\"three\"), 3);
}"
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))))

(defexpect coll-type-nested
  (do
    (import '[java.util List Map Set])
    (let [ast (az/analyze '(def ^{:mtype [Map [String [List [Character]]]]} numberSystemsMap {"LATIN" [\0 \1 \9]}))]
      (expect
"lazy_static! {
  static ref mut numberSystemsMap: HashMap<String,Vec<char>> = HashMap::new();
  numberSystemsMap.insert(String::from(\"LATIN\"), vec!['0', '1', '9']);
}"
              (emit (map->AstOpts {:ast ast :lang ::l/rust}))))))

(defexpect vectors-nested-impl-recursive-fn
  (let [ast (az/analyze '[2 3 5])
        type-class-ast {:mtype [java.util.List [java.lang.Integer]]}
        identifier "matrix"
        ast-opts (map->AstOpts {:ast ast :lang ::l/rust :impl-state {:type-class-ast type-class-ast
                                                                    :identifier identifier
                                                                    :position-vector [0]
                                                                    :statements []}})]
    (expect (rust-emit-assignment-vector-nested-recursive ast-opts)
            ["matrixV0" ["let matrixV0: Vec<i32> = vec![2, 3, 5];"]])))
