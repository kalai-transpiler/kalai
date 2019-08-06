(ns clj-icu-test.cpp-test
  (:require [clj-icu-test.core :refer :all]
            [clj-icu-test.cpp :refer :all]
            [clojure.tools.analyzer.jvm :as az]
            [expectations :refer :all])
  (:import clj_icu_test.core.AstOpts))

;;
;; C++
;;

(reset-indent-level)

;; types

;; types - numbers

(expect true (is-number-type? java.lang.Number))

(expect true (is-number-type? Number))

(expect true (is-number-type? java.lang.Short))

(expect true (is-number-type? java.lang.Integer))

(expect true (is-number-type? java.lang.Long))

(expect true (is-number-type? java.lang.Float))

(expect true (is-number-type? java.lang.Double))

(expect false (is-number-type? java.lang.Character))

(expect false (is-number-type? java.lang.Boolean))

;; bindings

;; bindings - def

(let [ast (az/analyze '(def x 3))]
  (expect "x = 3;" (emit-cpp (map->AstOpts {:ast ast}))))

(let [ast (az/analyze '(def ^Integer x 5))]
  (expect "int x = 5;" (emit-cpp (map->AstOpts {:ast ast}))))

;; language - multiple expressions

;; language - multiple expressions - do block

(let [ast (az/analyze '(do (def x 3) (def y 5)))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) ["x = 3;"
                                                "y = 5;"]))

(let [ast (az/analyze '(do (def ^Boolean x true) (def ^Long y 5)))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) ["bool x = true;"
                                                "long int y = 5;"]))

;; bindings

;; bindings - atoms

(let [ast (az/analyze '(def x (atom 11)))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "x = 11;"))

;; bindings - reset!

(let [ast (az/analyze '(do (def x (atom 11)) (reset! x 13)))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) ["x = 11;"
                                                "x = 13;"]))

(let [ast (az/analyze '(do (def ^Long x (atom 11)) (reset! x 13)))]
  (expect (emit-cpp {:ast ast}) ["long int x = 11;"
                                  "x = 13;"]))

;; static call (arithmetic operations)

;; static call - +

(let [ast (az/analyze '(+ 11 17))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "11 + 17"))

;; static call - /

(let [ast (az/analyze '(/ 34 17))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "34 / 17"))

;; static call - =

(let [ast (az/analyze '(= 34 (* 2 17)))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "34 == (2 * 17)"))

;; static call - convert fn names to symbols

(let [ast (az/analyze '(quot 37 10))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "37 / 10"))

(let [ast (az/analyze '(rem 37 10))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "37 % 10"))

;; language - multiple operands

(let [ast (az/analyze '(+ 11 17 19 23))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "11 + 17 + 19 + 23"))

;; bindings - let

;; bindings - let - 1 expression

(let [ast (az/analyze '(let [x 1] (+ x 3)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  x = 1;
  x + 3;
}"))

;; bindings - let - 1 expression - type signature on symbol

(let [ast (az/analyze '(let [^Integer x 1] (+ x 3)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  int x = 1;
  x + 3;
}"))

;; bindings - let - 2 expressions

(let [ast (az/analyze '(let [x 1] (+ x 3) (+ x 5)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  x = 1;
  x + 3;
  x + 5;
}"))

;; bindings - let - 2 bindings

(let [ast (az/analyze '(let [x 1 y 2] (* x y)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  x = 1;
  y = 2;
  x * y;
}"))

;; bindings - let - 2 bindings - expression in binding

(let [ast (az/analyze '(let [x 5 y (* x x)] (+ x y)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  x = 5;
  y = x * x;
  x + y;
}"))

;; bindings - let - nesting of let forms

(let [ast (az/analyze '(let [x 5] (let [y (* x x)] (/ y x))))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  x = 5;
  {
    y = x * x;
    y / x;
  }
}"))

;; bindings - let - atom (as bound value)

(let [ast (az/analyze '(let [a (atom 23)] (+ 3 5)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  a = 23;
  3 + 5;
}"))

;; bindings - let - atom (as bound value) and reset!

(let [ast (az/analyze '(let [a (atom 23)] (reset! a 19)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  a = 23;
  a = 19;
}"))

;; bindings - let - atom (as bound value) and reset! - type signature

(let [ast (az/analyze '(let [^Integer a (atom 23)] (reset! a 19)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  int a = 23;
  a = 19;
}"))

;; language - nested operands

(let [ast (az/analyze '(+ 3 5 (+ 1 7) 23))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "3 + 5 + (1 + 7) + 23"))

(let [ast (az/analyze '(/ 3 (/ 5 2) (/ 1 7) 23))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "3 / (5 / 2) / (1 / 7) / 23"))

(let [ast (az/analyze '(let [x 101] (+ 3 5 (+ x (+ 1 7 (+ x x))) 23)))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  x = 101;
  3 + 5 + (x + (1 + 7 + (x + x))) + 23;
}"))


(let [ast (az/analyze '(/ 3 (+ 5 2) (* 1 7) 23))]
  (expect (emit-cpp (map->AstOpts {:ast ast})) "3 / (5 + 2) / (1 * 7) / 23"))

;; defn

(let [ast (az/analyze '(defn compute ^void [^Integer x ^Integer y] (let [^Integer a (+ x y)] (* a y))))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"void compute(int x, int y)
{
  {
    int a = x + y;
    a * y;
  }
}"))


(let [ast (az/analyze '(defn doStuff ^void [^Integer x ^Integer y] (str (+ x y)) (println "hello") 3))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"void doStuff(int x, int y)
{
  std::to_string(x + y);
  cout << \"hello\" << endl;
  3;
}"))

;; classes

(let [ast (az/analyze '(defclass "MyClass" (def ^Integer b 3) (defn x ^void [] (+ b 1))))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"class MyClass
{
  int b = 3;

  void x()
  {
    b + 1;
  }
};"))

;; enums

(let [ast (az/analyze '(defenum "Day"
                         SUNDAY MONDAY TUESDAY WEDNESDAY THURSDAY FRIDAY SATURDAY))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"enum Day
{
  SUNDAY,
  MONDAY,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY
};"))

;; return statement

(let [ast (az/analyze '(defn add ^Integer [^Integer x ^Integer y]
                         (let [^Integer sum (+ x y)]
                           (return sum))))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"int add(int x, int y)
{
  {
    int sum = x + y;
    return sum;
  }
}"))

(let [ast (az/analyze '(defn add ^Integer [^Integer x ^Integer y]
                         (return (+ x y))))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"int add(int x, int y)
{
  return x + y;
}"))

;; deref

(let [ast (az/analyze '(let [x (atom 3)] @x))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  x = 3;
  x;
}"))


(let [ast (az/analyze '(let [x (atom 3)] x))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"{
  x = 3;
  x;
}"))

;; loops (ex: while, doseq)

(let [ast (az/analyze '(while true (println "e")))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"while (true)
{
  cout << \"e\" << endl;
}"))

;; not

(let [ast (az/analyze '(not (= 3 (/ 10 2))))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"!(3 == (10 / 2))"))

;; demo code

(let [ast (az/analyze '(defclass "NumFmt"
                         (defn format ^String [^Integer num]
                           (let [^Integer i (atom num)
                                 ^String result (atom "")]
                             (while (not (= @i 0))
                               (let [^Integer quotient (quot @i 10)
                                     ^Integer remainder (rem @i 10)]
                                 (reset! result (str remainder @result))
                                 (reset! i quotient)))
                             (return @result)))))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"class NumFmt
{
  string format(int num)
  {
    {
      int i = num;
      string result = \"\";
      while (!((i) == 0))
      {
        {
          int quotient = (i) / 10;
          int remainder = (i) % 10;
          result = std::to_string(remainder) + result;
          i = quotient;
        }
      }
      return result;
    }
  }
};"))

;; TODO: make emitters for args to a static call / function call invoke discard the parens around derefs.
;; Then this test should be removed, and test above can have a simplified output.
(let [ast (az/analyze '(defclass "NumFmt"
                         (defn format ^String [^Integer num]
                           (let [^Integer i (atom num)
                                 ^String result (atom "")]
                             (while (not (= i 0))
                               (let [^Integer quotient (quot i 10)
                                     ^Integer remainder (rem i 10)]
                                 (reset! result (str remainder result))
                                 (reset! i quotient)))
                             (return result)))))]
  (expect (emit-cpp (map->AstOpts {:ast ast}))
"class NumFmt
{
  string format(int num)
  {
    {
      int i = num;
      string result = \"\";
      while (!(i == 0))
      {
        {
          int quotient = i / 10;
          int remainder = i % 10;
          result = std::to_string(remainder) + result;
          i = quotient;
        }
      }
      return result;
    }
  }
};"))

