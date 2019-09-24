(ns clj-icu-test.emit.impl.util.cpp-type-util-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.impl.util.cpp-type-util :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]
            [expectations :refer :all])
  (:import clj_icu_test.common.AstOpts))


(let [ast (az/analyze '[2 3 5])
      ast-opts (map->AstOpts {:ast ast :lang ::l/cpp})
      type-class-ast {:mtype [java.util.List [java.lang.Integer]]}
      identifier "matrix"]
  (expect (cpp-emit-assignment-vector-nested-recursive ast-opts type-class-ast identifier [0] [])
          ["matrixV0" ["std::vector<int> matrixV0 = {2, 3, 5};"]]))

