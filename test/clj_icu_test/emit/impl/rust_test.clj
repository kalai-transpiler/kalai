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
    (expect "let x = 3;" (emit (map->AstOpts {:ast ast :lang ::l/rust}))))
  (let [ast (az/analyze '(def ^Integer x 5))]
    (expect "let x: i32 = 5;" (emit (map->AstOpts {:ast ast :lang ::l/rust})))))

;; language - multiple expressions

;; language - multiple expressions - do block

(defexpect lang-mult-expr-do-block
  (let [ast (az/analyze '(do (def x 3) (def y 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) ["let x = 3;"
                                                             "let y = 5;"])) 
  (let [ast (az/analyze '(do (def ^Boolean x true) (def ^Long y 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) ["let x: bool = true;"
                                                             "let y: i64 = 5;"])))

;; bindings

;; bindings - atoms

(defexpect bindings-atoms
  (let [ast (az/analyze '(def x (atom 11)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) "let mut x = 11;")))

;; bindings - reset!

(defexpect bindings-reset
  (let [ast (az/analyze '(do (def x (atom 11)) (reset! x 13)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) ["let mut x = 11;"
                                                             "x = 13;"]))

  (let [ast (az/analyze '(do (def ^Long x (atom 11)) (reset! x 13)))]
    (expect (emit {:ast ast :lang ::l/rust}) ["let mut x: i64 = 11;"
                                              "x = 13;"])))
