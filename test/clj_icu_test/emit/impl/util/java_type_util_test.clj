(ns clj-icu-test.emit.impl.util.java-type-util-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.tools.analyzer.jvm :as az] 
            [expectations.clojure.test :refer :all])
  (:import clj_icu_test.common.AstOpts))


(reset-indent-level)

(defexpect vectors
  (do
    (import 'java.util.List)
    (let [ast (az/analyze '(def ^{:mtype [List [Integer]]} numbers [13 17 19 23]))]
      (expect "List<Integer> numbers = Arrays.asList(13, 17, 19, 23);"
              (emit (map->AstOpts {:ast ast :lang ::l/java}))))
    (let [ast (az/analyze '(do (def x 13) (def ^{:mtype [List [Integer]]} numbers [x])))]
      (expect ["x = 13;"
               "List<Integer> numbers = Arrays.asList(x);"]
              (emit (map->AstOpts {:ast ast :lang ::l/java}))))
    
    (let [ast (az/analyze '(def ^{:mtype [List [List [Integer]]]} matrix [[2 3 5]
                                                                          [7 11 13]
                                                                          [17 19 23]]))]
      (expect "List<List<Integer>> matrix = Arrays.asList(Arrays.asList(2, 3, 5), Arrays.asList(7, 11, 13), Arrays.asList(17, 19, 23));"
              (emit (map->AstOpts {:ast ast :lang ::l/java}))))))

;; add test with a map as a return type

(defexpect maps
  ;; (let [ast (az/analyze '{"one" 1
  ;;                         "two" 2
  ;;                         "three" 3})]
  ;;   )
  (do
    (import 'java.util.Map)
    (let [ast (az/analyze '(def ^{:mtype [Map [String Integer]]} number-words {"one" 1
                                                                               "two" 2
                                                                               "three" 3}))]
      "TODO")))

(defexpect sets
  (do
    (import 'java.util.Set)
    (let [ast (az/analyze '#{"smell" "sight" "touch" "taste" "hearing"})]
      )
    (let [ast (az/analyze '(def ^{:mtype [Set [String]]} senses #{"smell" "sight" "touch" "taste" "hearing"}))]
      "TODO")))

(defexpect coll-type-nested
  (do
    (import '[java.util List Map Set])
    (let [ast (az/analyze '(def ^{:mtype [Map [String List [Character]]]} number-systems-map {}))]
      "TODO")))
