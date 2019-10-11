(ns clj-icu-test.emit.impl.util.cpp-type-util-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.impl.util.cpp-type-util :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
  (:import clj_icu_test.common.AstOpts))


(reset-indent-level)

(defexpect scalar
  (defexpect character
    (let [ast (az/analyze '\0)]
      (expect "'0'"
              (emit (map->AstOpts {:ast ast :lang ::l/cpp}))))))

(defexpect vectors
  (do
    (import 'java.util.List)
    (let [ast (az/analyze '(def ^{:mtype [List [Integer]]} numbers [13 17 19 23]))]
      (expect "std::vector<int> numbers = {13, 17, 19, 23};"
              (emit (map->AstOpts {:ast ast :lang ::l/cpp}))))
    (let [ast (az/analyze '(do (def x 13) (def ^{:mtype [List [Integer]]} numbers [x])))]
      (expect ["x = 13;"
               "std::vector<int> numbers = {x};"]
              (emit (map->AstOpts {:ast ast :lang ::l/cpp}))))
    (let [ast (az/analyze '(def ^{:mtype [List [List [Integer]]]} matrix [[2 3 5]
                                                                          [7 11 13]
                                                                          [17 19 23]]))]
      (expect ["std::vector<int> matrixV0 = {2, 3, 5};"
               "std::vector<int> matrixV1 = {7, 11, 13};"
               "std::vector<int> matrixV2 = {17, 19, 23};"
               "std::vector<std::vector<int>> matrix = {matrixV0, matrixV1, matrixV2};"]
              (emit (map->AstOpts {:ast ast :lang ::l/cpp}))))))

(defexpect maps
  ;; (let [ast (az/analyze '{"one" 1
  ;;                         "two" 2
  ;;                         "three" 3})]
  ;;   )
  (do
    (import 'java.util.Map)
    (let [ast (az/analyze '(def ^{:mtype [Map [String Integer]]} numberWords {"one" 1
                                                                              "two" 2
                                                                              "three" 3}))]
      (expect ["std::map<std::string,int> numberWords;"
               "numberWords.insert(std::make_pair(\"one\", 1));"
               "numberWords.insert(std::make_pair(\"two\", 2));"
               "numberWords.insert(std::make_pair(\"three\", 3));"]
              (emit (map->AstOpts {:ast ast :lang ::l/cpp}))))))

(defexpect sets
  (do
    (import 'java.util.Set)
    ;; (let [ast (az/analyze '#{"smell" "sight" "touch" "taste" "hearing"})]
    ;;   )
    (let [ast (az/analyze '(def ^{:mtype [Set [String]]} senses #{"smell" "sight" "touch" "taste" "hearing"}))]
      "TODO")))

(defexpect coll-type-nested
  (do
    (import '[java.util List Map Set])
    (let [ast (az/analyze '(def ^{:mtype [Map [String List [Character]]]} number-systems-map {}))]
      "TODO")))

(defexpect vectors-nested-impl-recursive-fn
  (let [ast (az/analyze '[2 3 5])
        ast-opts (map->AstOpts {:ast ast :lang ::l/cpp})
        type-class-ast {:mtype [java.util.List [java.lang.Integer]]}
        identifier "matrix"]
    (expect (cpp-emit-assignment-vector-nested-recursive ast-opts type-class-ast identifier [0] [])
            ["matrixV0" ["std::vector<int> matrixV0 = {2, 3, 5};"]])))

