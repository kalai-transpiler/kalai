(ns clj-icu-test.emit.impl.curlybrace-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
  (:import clj_icu_test.common.AstOpts))


(defexpect emit-arg-expressions-in-arg
  (let [ast (az/analyze '(+ 3 5 (+ 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "3 + 5 + (1 + 7) + 23"))
  (let [ast (az/analyze '(/ 3 (/ 5 2) (/ 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "3 / (5 / 2) / (1 / 7) / 23"))
  (let [ast (az/analyze '(/ 3 (+ 5 2) (* 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "3 / (5 + 2) / (1 * 7) / 23")))

(defexpect emit-arg-collection-as-arg
  (let [ast (az/analyze '[3 5 101])
        ast-opts (map->AstOpts {:ast ast :lang ::l/java})]
    (expect (emit-arg ast-opts '[3 5 101]) "Arrays.asList(3, 5, 101)"))
  (let [ast (az/analyze '[3 5 [1 7] 23])
        ast-opts (map->AstOpts {:ast ast :lang ::l/java})]
    (expect (emit-arg ast-opts '[3 5 [1 7] 23]) "Arrays.asList(3, 5, Arrays.asList(1, 7), 23)")))
