(ns clj-icu-test.emit.impl.rust-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
  (:import clj_icu_test.common.AstOpts))

;;
;; Rust
;;

(reset-indent-level)

;; bindings

;; bindings - def

(defexpect bindings-def 
  (let [ast (az/analyze '(def x 3))]
    (expect "x = 3;" (emit (map->AstOpts {:ast ast :lang ::l/rust}))))
  (let [ast (az/analyze '(def ^Integer x 5))]
    (expect "let x: i32 = 5;" (emit (map->AstOpts {:ast ast :lang ::l/rust})))))
