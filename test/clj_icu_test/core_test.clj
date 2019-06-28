(ns clj-icu-test.core-test
  (:require ;;[clojure.test :refer :all]
            [clojure.tools.analyzer.jvm :as az]
            [clj-icu-test.core :refer :all]
            [expectations :refer :all]))
;;
;; C++
;;

;; bindings

;; bindings - def

(let [ast (az/analyze '(def x 3))]
  (expect "x = 3;" (emit-cpp ast)))

(let [ast (az/analyze '(def ^Integer x 5))]
  (expect "int x = 5;" (emit-cpp ast)))

;;
;; Java
;;

;; bindings

;; bindings - def

(let [ast (az/analyze '(def x 3))] 
  (expect "x = 3;" (emit-java ast)))

(let [ast (az/analyze '(def ^Integer x 5))]
  (expect "Integer x = 5;" (emit-java ast)))

;; language - multiple expressions

;; language - multiple expressions - do block

(let [ast (az/analyze '(do (def x 3) (def y 5)))]
  (expect (emit-java ast) ["x = 3;"
                           "y = 5;"]))

(let [ast (az/analyze '(do (def ^Boolean x true) (def ^Long y 5)))]
  (expect (emit-java ast) ["Boolean x = true;"
                           "Long y = 5;"]))

;; bindings

;; bindings - atoms

(let [ast (az/analyze '(def x (atom 11)))]
  (expect (emit-java ast) "x = 11;"))

;; bindings - reset!

(let [ast (az/analyze '(do (def x (atom 11)) (reset! x 13)))]
  (expect (emit-java ast) ["x = 11;"
                           "x = 13;"]))

(let [ast (az/analyze '(do (def ^Long x (atom 11)) (reset! x 13)))]
  (expect (emit-java ast) ["Long x = 11;"
                           "x = 13;"]))

;; static call (arithmetic operations)

;; static call - +

(let [ast (az/analyze '(+ 11 17))]
  (expect (emit-java ast) "11 + 17"))

;; static call - /

(let [ast (az/analyze '(/ 34 17))]
  (expect (emit-java ast) "34 / 17"))

;; language - multiple operands

(let [ast (az/analyze '(+ 11 17 19 23))]
  (expect (emit-java ast) "11 + 17 + 19 + 23"))

;; bindings - let

;; bindings - let - 1 expression

(let [ast (az/analyze '(let [x 1] (+ x 3)))]
  (expect (emit-java ast)
"{
  x = 1;
  x + 3;
}"))

;; bindings - let - 2 expressions

(let [ast (az/analyze '(let [x 1] (+ x 3) (+ x 5)))]
  (expect (emit-java ast)
"{
  x = 1;
  x + 3;
  x + 5;
}"))

;; bindings - let - 2 bindings

(let [ast (az/analyze '(let [x 1 y 2] (* x y)))]
  (expect (emit-java ast)
"{
  x = 1;
  y = 2;
  x * y;
}"))

;; bindings - let - 2 bindings - expression in binding

(let [ast (az/analyze '(let [x 5 y (* x x)] (+ x y)))]
  (expect (emit-java ast)
"{
  x = 5;
  y = x * x;
  x + y;
}"))

;; bindings - let - nesting of let forms

(let [ast (az/analyze '(let [x 5] (let [y (* x x)] (/ y x))))]
  (expect (emit-java ast)
"{
  x = 5;
  {
    y = x * x;
    y / x;
  }
}"))

;; bindings - let - atom (as bound value)

(let [ast (az/analyze '(let [a (atom 23)] (+ 3 5)))]
  (expect (emit-java ast)
"{
  a = 23;
  3 + 5;
}"))
