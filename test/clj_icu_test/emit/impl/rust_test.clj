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

;; bindings - let

;; bindings - let - 1 expression

(defexpect bindings-let-1-expr
  (let [ast (az/analyze '(let [x 1] (+ x 3)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 1;
  x + 3;
}")))

;; bindings - let - 1 expression - type signature on symbol

(defexpect bindings-let-1-expr-type-signature
  (let [ast (az/analyze '(let [^Integer x 1] (+ x 3)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x: i32 = 1;
  x + 3;
}")))

;; bindings - let - 2 expressions

(defexpect bindings-let-2-expr
  (let [ast (az/analyze '(let [x 1] (+ x 3) (+ x 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 1;
  x + 3;
  x + 5;
}")))

;; bindings - let - 2 bindings

(defexpect bindings-let-2-bindings
  (let [ast (az/analyze '(let [x 1 y 2] (* x y)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 1;
  let y = 2;
  x * y;
}")))

;; bindings - let - 2 bindings - expression in binding

(defexpect bindings-let-2-bindings-with-exprs
  (let [ast (az/analyze '(let [x 5 y (* x x)] (+ x y)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 5;
  let y = x * x;
  x + y;
}")))

;; bindings - let - nesting of let forms

(defexpect bindings-let-nested
  (let [ast (az/analyze '(let [x 5] (let [y (* x x)] (/ y x))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 5;
  {
    let y = x * x;
    y / x;
  }
}")))

;; bindings - let - atom (as bound value)

(defexpect bindings-let-atom
  (let [ast (az/analyze '(let [a (atom 23)] (+ 3 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut a = 23;
  3 + 5;
}")))

;; bindings - let - atom (as bound value) and reset!

(defexpect bindings-let-atom-with-reset
  (let [ast (az/analyze '(let [a (atom 23)] (reset! a 19)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut a = 23;
  a = 19;
}")))

;; bindings - let - atom (as bound value) and reset! - type signature

(defexpect bindings-let-atom-with-reset-type-signature
  (let [ast (az/analyze '(let [^Integer a (atom 23)] (reset! a 19)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let mut a: i32 = 23;
  a = 19;
}")))

;; language - nested operands

(defexpect lang-nested-operands
  (let [ast (az/analyze '(+ 3 5 (+ 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) "3 + 5 + &(1 + 7) + 23"))
  (let [ast (az/analyze '(/ 3 (/ 5 2) (/ 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) "3 / &(5 / 2) / &(1 / 7) / 23"))
  (let [ast (az/analyze '(let [x 101] (+ 3 5 (+ x (+ 1 7 (+ x x))) 23)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"{
  let x = 101;
  3 + 5 + &(x + &(1 + 7 + &(x + x))) + 23;
}"))


  (let [ast (az/analyze '(/ 3 (+ 5 2) (* 1 7) 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust})) "3 / &(5 + 2) / &(1 * 7) / 23")))

;; defn

(defexpect defn-test
  (let [ast (az/analyze '(defn compute ^void [^Integer x ^Integer y] (let [^Integer a (+ x y)] (* a y))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"pub fn compute(x: &i32, y: &i32)
{
  {
    let a: i32 = x + y;
    a * y;
  }
}"))

  (let [ast (az/analyze '(defn doStuff ^void [^Integer x ^Integer y] (str (+ x y)) (println "hello") 3))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"pub fn doStuff(x: &i32, y: &i32)
{
  format!(\"{}\", (x + y).to_string());
  println!(\"{}\", format!(\"{}\", String::from(\"hello\")));
  3;
}"))

  (let [ast (az/analyze '(defn returnStuff ^Integer [^Integer x ^Integer y] (let [^Integer a (+ x y)] (return a))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"pub fn returnStuff(x: &i32, y: &i32) -> i32
{
  {
    let a: i32 = x + y;
    return a;
  }
}")))

;; classes

(defexpect classes
  (do
    (require '[clj-icu-test.common :refer :all])
    (let [ast (az/analyze '(defclass "MyClass" (def ^Integer b 3) (defn x ^void [] (+ b 1))))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"let b: i32 = 3;

pub fn x()
{
  b + 1;
}"))))

;; enums

(defexpect enums
  (do
    (require '[clj-icu-test.common :refer :all])
    (let [ast (az/analyze '(defenum "Day"
                             SUNDAY MONDAY TUESDAY WEDNESDAY THURSDAY FRIDAY SATURDAY))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"enum Day
{
  SUNDAY,
  MONDAY,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY
}"))))

;; fn invocations

(defexpect strlen-test
  (let [ast (az/analyze '(do (def ^String s "Hello, Martians") (strlen s)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            ["let s: String = String::from(\"Hello, Martians\");"
             "s.len();"])))

;; loops (ex: while, doseq)

(defexpect loops
  (let [ast (az/analyze '(while true (println "e")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
"while (true)
{
  println!(\"{}\", format!(\"{}\", String::from(\"e\")));
}")))

;; other built-in fns (also marked with op = :static-call)

(defexpect get-test
  (let [ast (az/analyze '(do (def ^{:mtype [Map [String Integer]]} numberWords {"one" 1
                                                                                "two" 2
                                                                                "three" 3})
                             (get numberWords "one")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/rust}))
            ["let mut numberWords: HashMap<String,i32> = HashMap::new();
numberWords.insert(String::from(\"one\"), 1);
numberWords.insert(String::from(\"two\"), 2);
numberWords.insert(String::from(\"three\"), 3);"
             "*numberWords.get(&String::from(\"one\")).unwrap();"])))
