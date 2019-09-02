(ns clj-icu-test.cpp.cpp-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.tools.analyzer.jvm :as az]
            [expectations :refer :all])
  (:import clj_icu_test.common.AstOpts))

;;
;; C++
;;

(reset-indent-level)

;; types

;; types - numbers


;; TODO: turn util fns in commented-out tests into multimethods

;; (expect true (is-number-type? java.lang.Number))

;; (expect true (is-number-type? Number))

;; (expect true (is-number-type? java.lang.Short))

;; (expect true (is-number-type? java.lang.Integer))

;; (expect true (is-number-type? java.lang.Long))

;; (expect true (is-number-type? java.lang.Float))

;; (expect true (is-number-type? java.lang.Double))

;; (expect false (is-number-type? java.lang.Character))

;; (expect false (is-number-type? java.lang.Boolean))



;; bindings

;; bindings - def

(let [ast (az/analyze '(def x 3))]
  (expect "x = 3;" (emit (map->AstOpts {:ast ast :lang ::l/cpp}))))

(let [ast (az/analyze '(def ^Integer x 5))]
  (expect "int x = 5;" (emit (map->AstOpts {:ast ast :lang ::l/cpp}))))

;; language - multiple expressions

;; language - multiple expressions - do block

(let [ast (az/analyze '(do (def x 3) (def y 5)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) ["x = 3;"
                                                "y = 5;"]))

(let [ast (az/analyze '(do (def ^Boolean x true) (def ^Long y 5)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) ["bool x = true;"
                                                "long int y = 5;"]))

;; bindings

;; bindings - atoms

(let [ast (az/analyze '(def x (atom 11)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "x = 11;"))

;; bindings - reset!

(let [ast (az/analyze '(do (def x (atom 11)) (reset! x 13)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) ["x = 11;"
                                                "x = 13;"]))

(let [ast (az/analyze '(do (def ^Long x (atom 11)) (reset! x 13)))]
  (expect (emit {:ast ast :lang ::l/cpp}) ["long int x = 11;"
                                  "x = 13;"]))

;; static call (arithmetic operations)

;; static call - +

(let [ast (az/analyze '(+ 11 17))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "11 + 17"))

;; static call - /

(let [ast (az/analyze '(/ 34 17))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "34 / 17"))

;; static call - =

(let [ast (az/analyze '(= 34 (* 2 17)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "34 == (2 * 17)"))

;; static call - convert fn names to symbols

(let [ast (az/analyze '(quot 37 10))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "37 / 10"))

(let [ast (az/analyze '(rem 37 10))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "37 % 10"))

;; language - multiple operands

(let [ast (az/analyze '(+ 11 17 19 23))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "11 + 17 + 19 + 23"))

;; bindings - let

;; bindings - let - 1 expression

(let [ast (az/analyze '(let [x 1] (+ x 3)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 1;
  x + 3;
}"))

;; bindings - let - 1 expression - type signature on symbol

(let [ast (az/analyze '(let [^Integer x 1] (+ x 3)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  int x = 1;
  x + 3;
}"))

;; bindings - let - 2 expressions

(let [ast (az/analyze '(let [x 1] (+ x 3) (+ x 5)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 1;
  x + 3;
  x + 5;
}"))

;; bindings - let - 2 bindings

(let [ast (az/analyze '(let [x 1 y 2] (* x y)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 1;
  y = 2;
  x * y;
}"))

;; bindings - let - 2 bindings - expression in binding

(let [ast (az/analyze '(let [x 5 y (* x x)] (+ x y)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 5;
  y = x * x;
  x + y;
}"))

;; bindings - let - nesting of let forms

(let [ast (az/analyze '(let [x 5] (let [y (* x x)] (/ y x))))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 5;
  {
    y = x * x;
    y / x;
  }
}"))

;; bindings - let - atom (as bound value)

(let [ast (az/analyze '(let [a (atom 23)] (+ 3 5)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  a = 23;
  3 + 5;
}"))

;; bindings - let - atom (as bound value) and reset!

(let [ast (az/analyze '(let [a (atom 23)] (reset! a 19)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  a = 23;
  a = 19;
}"))

;; bindings - let - atom (as bound value) and reset! - type signature

(let [ast (az/analyze '(let [^Integer a (atom 23)] (reset! a 19)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  int a = 23;
  a = 19;
}"))

;; language - nested operands

(let [ast (az/analyze '(+ 3 5 (+ 1 7) 23))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "3 + 5 + (1 + 7) + 23"))

(let [ast (az/analyze '(/ 3 (/ 5 2) (/ 1 7) 23))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "3 / (5 / 2) / (1 / 7) / 23"))

(let [ast (az/analyze '(let [x 101] (+ 3 5 (+ x (+ 1 7 (+ x x))) 23)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 101;
  3 + 5 + (x + (1 + 7 + (x + x))) + 23;
}"))


(let [ast (az/analyze '(/ 3 (+ 5 2) (* 1 7) 23))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "3 / (5 + 2) / (1 * 7) / 23"))

;; defn

(let [ast (az/analyze '(defn compute ^void [^Integer x ^Integer y] (let [^Integer a (+ x y)] (* a y))))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"void compute(int x, int y)
{
  {
    int a = x + y;
    a * y;
  }
}"))


(let [ast (az/analyze '(defn doStuff ^void [^Integer x ^Integer y] (str (+ x y)) (println "hello") 3))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"void doStuff(int x, int y)
{
  std::to_string(x + y);
  cout << \"hello\" << endl;
  3;
}"))

;; classes

(let [ast (az/analyze '(defclass "MyClass" (def ^Integer b 3) (defn x ^void [] (+ b 1))))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
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
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
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
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"int add(int x, int y)
{
  {
    int sum = x + y;
    return sum;
  }
}"))

(let [ast (az/analyze '(defn add ^Integer [^Integer x ^Integer y]
                         (return (+ x y))))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"int add(int x, int y)
{
  return x + y;
}"))

;; deref

(let [ast (az/analyze '(let [x (atom 3)] @x))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 3;
  x;
}"))


(let [ast (az/analyze '(let [x (atom 3)] x))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 3;
  x;
}"))

;; loops (ex: while, doseq)

(let [ast (az/analyze '(while true (println "e")))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"while (true)
{
  cout << \"e\" << endl;
}"))

;; not

(let [ast (az/analyze '(not (= 3 (/ 10 2))))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"!(3 == (10 / 2))"))

;; new

(let [ast (az/analyze '(StringBuffer.))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
          "StringBuffer"))

(let [ast (az/analyze '(StringBuffer. "Initial string value"))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
          "StringBuffer(\"Initial string value\")"))

;; string buffer - new

(let [ast (az/analyze '(new-strbuf))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
          "\"\""))

(let [ast (az/analyze '(atom (new-strbuf)))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
          "\"\""))

(let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] sb))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  std::string sb = \"\";
  sb;
}"))

;; string buffer - prepend


(let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] (prepend-strbuf sb "hello")))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  std::string sb = \"\";
  \"hello\" + sb;
}"))

;; demo code

(let [ast (az/analyze '(defclass "NumFmt"
                         (defn format ^String [^Integer num]
                           (let [^Integer i (atom num)
                                 ^StringBuffer result (atom (new-strbuf))]
                             (while (not (= @i 0))
                               (let [^Integer quotient (quot @i 10)
                                     ^Integer remainder (rem @i 10)]
                                 (reset! result (prepend-strbuf @result remainder))
                                 (reset! i quotient)))
                             (return (tostring-strbuf @result))))))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"class NumFmt
{
  std::string format(int num)
  {
    {
      int i = num;
      std::string result = \"\";
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
                                 ^StringBuffer result (atom (new-strbuf))]
                             (while (not (= i 0))
                               (let [^Integer quotient (quot i 10)
                                     ^Integer remainder (rem i 10)]
                                 (reset! result (prepend-strbuf @result remainder))
                                 (reset! i quotient)))
                             (return (tostring-strbuf @result))))))]
  (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"class NumFmt
{
  std::string format(int num)
  {
    {
      int i = num;
      std::string result = \"\";
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

