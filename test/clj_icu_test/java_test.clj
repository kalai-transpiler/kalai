(ns clj-icu-test.java-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.java :refer :all]
            [clojure.tools.analyzer.jvm :as az]
            [expectations :refer :all])
  (:import clj_icu_test.common.AstOpts))

;;
;; Java
;;

(reset-indent-level)

;; bindings

;; bindings - def

(let [ast (az/analyze '(def x 3))] 
  (expect "x = 3;" (emit-java (map->AstOpts {:ast ast :lang "java"}))))

(let [ast (az/analyze '(def ^Integer x 5))]
  (expect "Integer x = 5;" (emit-java (map->AstOpts {:ast ast :lang "java"}))))

;; language - multiple expressions

;; language - multiple expressions - do block

(let [ast (az/analyze '(do (def x 3) (def y 5)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) ["x = 3;"
                                                 "y = 5;"]))

(let [ast (az/analyze '(do (def ^Boolean x true) (def ^Long y 5)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) ["Boolean x = true;"
                                                 "Long y = 5;"]))

;; bindings

;; bindings - atoms

(let [ast (az/analyze '(def x (atom 11)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "x = 11;"))

;; bindings - reset!

(let [ast (az/analyze '(do (def x (atom 11)) (reset! x 13)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) ["x = 11;"
                                                 "x = 13;"]))

(let [ast (az/analyze '(do (def ^Long x (atom 11)) (reset! x 13)))]
  (expect (emit-java {:ast ast :lang "java"}) ["Long x = 11;"
                                  "x = 13;"]))

;; static call (arithmetic operations)

;; static call - +

(let [ast (az/analyze '(+ 11 17))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "11 + 17"))

;; static call - /

(let [ast (az/analyze '(/ 34 17))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "34 / 17"))

;; static call - =

(let [ast (az/analyze '(= 34 (* 2 17)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "34 == (2 * 17)"))

;; static call - convert fn names to symbols

(let [ast (az/analyze '(quot 37 10))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "37 / 10"))

(let [ast (az/analyze '(rem 37 10))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "37 % 10"))

;; language - multiple operands

(let [ast (az/analyze '(+ 11 17 19 23))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "11 + 17 + 19 + 23"))

;; bindings - let

;; bindings - let - 1 expression

(let [ast (az/analyze '(let [x 1] (+ x 3)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  x = 1;
  x + 3;
}"))

;; bindings - let - 1 expression - type signature on symbol

(let [ast (az/analyze '(let [^Integer x 1] (+ x 3)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  Integer x = 1;
  x + 3;
}"))

;; bindings - let - 2 expressions

(let [ast (az/analyze '(let [x 1] (+ x 3) (+ x 5)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  x = 1;
  x + 3;
  x + 5;
}"))

;; bindings - let - 2 bindings

(let [ast (az/analyze '(let [x 1 y 2] (* x y)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  x = 1;
  y = 2;
  x * y;
}"))

;; bindings - let - 2 bindings - expression in binding

(let [ast (az/analyze '(let [x 5 y (* x x)] (+ x y)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  x = 5;
  y = x * x;
  x + y;
}"))

;; bindings - let - nesting of let forms

(let [ast (az/analyze '(let [x 5] (let [y (* x x)] (/ y x))))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  x = 5;
  {
    y = x * x;
    y / x;
  }
}"))

;; bindings - let - atom (as bound value)

(let [ast (az/analyze '(let [a (atom 23)] (+ 3 5)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  a = 23;
  3 + 5;
}"))

;; bindings - let - atom (as bound value) and reset!

(let [ast (az/analyze '(let [a (atom 23)] (reset! a 19)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  a = 23;
  a = 19;
}"))

;; bindings - let - atom (as bound value) and reset! - type signature

(let [ast (az/analyze '(let [^Integer a (atom 23)] (reset! a 19)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  Integer a = 23;
  a = 19;
}"))

;; language - nested operands

(let [ast (az/analyze '(+ 3 5 (+ 1 7) 23))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "3 + 5 + (1 + 7) + 23"))

(let [ast (az/analyze '(/ 3 (/ 5 2) (/ 1 7) 23))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "3 / (5 / 2) / (1 / 7) / 23"))

(let [ast (az/analyze '(let [x 101] (+ 3 5 (+ x (+ 1 7 (+ x x))) 23)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  x = 101;
  3 + 5 + (x + (1 + 7 + (x + x))) + 23;
}"))


(let [ast (az/analyze '(/ 3 (+ 5 2) (* 1 7) 23))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"})) "3 / (5 + 2) / (1 * 7) / 23"))

;; defn

(let [ast (az/analyze '(defn compute ^void [^Integer x ^Integer y] (let [^Integer a (+ x y)] (* a y))))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"public void compute(Integer x, Integer y)
{
  {
    Integer a = x + y;
    a * y;
  }
}"))


(let [ast (az/analyze '(defn doStuff ^void [^Integer x ^Integer y] (str (+ x y)) (println "hello") 3))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"public void doStuff(Integer x, Integer y)
{
  new StringBuffer().append(x + y).toString();
  System.out.println(\"\" + \"hello\");
  3;
}"))

;; classes

(let [ast (az/analyze '(defclass "MyClass" (def ^Integer b 3) (defn x ^void [] (+ b 1))))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"public class MyClass
{
  Integer b = 3;

  public void x()
  {
    b + 1;
  }
}"))

;; enums

(let [ast (az/analyze '(defenum "Day"
                         SUNDAY MONDAY TUESDAY WEDNESDAY THURSDAY FRIDAY SATURDAY))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"public enum Day
{
  SUNDAY,
  MONDAY,
  TUESDAY,
  WEDNESDAY,
  THURSDAY,
  FRIDAY,
  SATURDAY
}"))

;; return statement

(let [ast (az/analyze '(defn add ^Integer [^Integer x ^Integer y]
                         (let [^Integer sum (+ x y)]
                           (return sum))))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"public Integer add(Integer x, Integer y)
{
  {
    Integer sum = x + y;
    return sum;
  }
}"))

(let [ast (az/analyze '(defn add ^Integer [^Integer x ^Integer y]
                         (return (+ x y))))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"public Integer add(Integer x, Integer y)
{
  return x + y;
}"))

;; deref

(let [ast (az/analyze '(let [x (atom 3)] @x))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  x = 3;
  x;
}"))


(let [ast (az/analyze '(let [x (atom 3)] x))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  x = 3;
  x;
}"))

;; loops (ex: while, doseq)

(let [ast (az/analyze '(while true (println "e")))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"while (true)
{
  System.out.println(\"\" + \"e\");
}"))

;; not

(let [ast (az/analyze '(not (= 3 (/ 10 2))))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
          "!(3 == (10 / 2))"))

;; new

(let [ast (az/analyze '(StringBuffer.))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
          "new StringBuffer()"))

(let [ast (az/analyze '(StringBuffer. "Initial string value"))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
          "new StringBuffer(\"Initial string value\")"))

;; string buffer - new

(let [ast (az/analyze '(new-strbuf))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
          "new StringBuffer()"))

(let [ast (az/analyze '(atom (new-strbuf)))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
          "new StringBuffer()"))

(let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] sb))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  StringBuffer sb = new StringBuffer();
  sb;
}"))

;; string buffer - prepend


(let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] (prepend-strbuf sb "hello")))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"{
  StringBuffer sb = new StringBuffer();
  sb.insert(0, \"hello\");
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
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"public class NumFmt
{
  public String format(Integer num)
  {
    {
      Integer i = num;
      StringBuffer result = new StringBuffer();
      while (!((i) == 0))
      {
        {
          Integer quotient = (i) / 10;
          Integer remainder = (i) % 10;
          result = result.insert(0, remainder);
          i = quotient;
        }
      }
      return result.toString();
    }
  }
}"))

;; TODO: make emitters for args to a static call / function call invoke discard the parens around derefs.
;; Then this test should be removed, and test above can have a simplified output.
(let [ast (az/analyze '(defclass "NumFmt"
                         (defn format ^String [^Integer num]
                           (let [^Integer i (atom num)
                                 ^StringBuffer result (atom (new-strbuf))]
                             (while (not (= i 0))
                               (let [^Integer quotient (quot i 10)
                                     ^Integer remainder (rem i 10)]
                                 (reset! result (prepend-strbuf result remainder))
                                 (reset! i quotient)))
                             (return (tostring-strbuf result))))))]
  (expect (emit-java (map->AstOpts {:ast ast :lang "java"}))
"public class NumFmt
{
  public String format(Integer num)
  {
    {
      Integer i = num;
      StringBuffer result = new StringBuffer();
      while (!(i == 0))
      {
        {
          Integer quotient = i / 10;
          Integer remainder = i % 10;
          result = result.insert(0, remainder);
          i = quotient;
        }
      }
      return result.toString();
    }
  }
}"))
