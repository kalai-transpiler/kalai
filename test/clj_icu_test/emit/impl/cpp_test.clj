(ns clj-icu-test.emit.impl.cpp-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
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

(defexpect bindings-def 
  (let [ast (az/analyze '(def x 3))]
    (expect "x = 3;" (emit (map->AstOpts {:ast ast :lang ::l/cpp})))) 
  (let [ast (az/analyze '(def ^Integer x 5))]
    (expect "int x = 5;" (emit (map->AstOpts {:ast ast :lang ::l/cpp})))))

;; language - multiple expressions

;; language - multiple expressions - do block

(defexpect lang-mult-expr-do-block
  (let [ast (az/analyze '(do (def x 3) (def y 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) ["x = 3;"
                                                            "y = 5;"])) 
  (let [ast (az/analyze '(do (def ^Boolean x true) (def ^Long y 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) ["bool x = true;"
                                                            "long int y = 5;"])))

;; bindings

;; bindings - atoms

(defexpect bindings-atoms
  (let [ast (az/analyze '(def x (atom 11)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "x = 11;")))

;; bindings - reset!

(defexpect bindings-reset
  (let [ast (az/analyze '(do (def x (atom 11)) (reset! x 13)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) ["x = 11;"
                                                            "x = 13;"]))

  (let [ast (az/analyze '(do (def ^Long x (atom 11)) (reset! x 13)))]
    (expect (emit {:ast ast :lang ::l/cpp}) ["long int x = 11;"
                                             "x = 13;"])))

;; bindings - let

;; bindings - let - 1 expression

(defexpect bindings-let-1-expr
  (let [ast (az/analyze '(let [x 1] (+ x 3)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 1;
  x + 3;
}")))

;; bindings - let - 1 expression - type signature on symbol

(defexpect bindings-let-1-expr-type-signature
  (let [ast (az/analyze '(let [^Integer x 1] (+ x 3)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  int x = 1;
  x + 3;
}")))

;; bindings - let - 2 expressions

(defexpect bindings-let-2-expr
  (let [ast (az/analyze '(let [x 1] (+ x 3) (+ x 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 1;
  x + 3;
  x + 5;
}")))

;; bindings - let - 2 bindings

(defexpect bindings-let-2-bindings
  (let [ast (az/analyze '(let [x 1 y 2] (* x y)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 1;
  y = 2;
  x * y;
}")))

;; bindings - let - 2 bindings - expression in binding

(defexpect bindings-let-2-bindings-with-exprs
  (let [ast (az/analyze '(let [x 5 y (* x x)] (+ x y)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 5;
  y = x * x;
  x + y;
}")))

;; bindings - let - nesting of let forms

(defexpect bindings-let-nested
  (let [ast (az/analyze '(let [x 5] (let [y (* x x)] (/ y x))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  x = 5;
  {
    y = x * x;
    y / x;
  }
}")))

;; bindings - let - atom (as bound value)

(defexpect bindings-let-atom
  (let [ast (az/analyze '(let [a (atom 23)] (+ 3 5)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  a = 23;
  3 + 5;
}")))

;; bindings - let - atom (as bound value) and reset!

(defexpect bindings-let-atom-with-reset
  (let [ast (az/analyze '(let [a (atom 23)] (reset! a 19)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  a = 23;
  a = 19;
}")))

;; bindings - let - atom (as bound value) and reset! - type signature

(defexpect bindings-let-atom-with-reset-type-signature
  (let [ast (az/analyze '(let [^Integer a (atom 23)] (reset! a 19)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  int a = 23;
  a = 19;
}")))

;; language - nested operands

(defexpect lang-nested-operands
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
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "3 / (5 + 2) / (1 * 7) / 23")))

;; defn

(defexpect defn-test
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

  (let [ast (az/analyze '(defn returnStuff ^Integer [^Integer x ^Integer y] (let [^Integer a (+ x y)] (return a))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"int returnStuff(int x, int y)
{
  {
    int a = x + y;
    return a;
  }
}")))

;; classes

(defexpect classes
  (do
    (require '[clj-icu-test.common :refer :all])
    (let [ast (az/analyze '(defclass "MyClass" (def ^Integer b 3) (defn x ^void [] (+ b 1))))]
      (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"class MyClass
{
  int b = 3;

  void x()
  {
    b + 1;
  }
};"))))

;; enums

(defexpect enums
  (do
    (require '[clj-icu-test.common :refer :all])
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
};"))))

;; fn invocations

(defexpect strlen-test
  (let [ast (az/analyze '(do (def ^String s "Hello, Martians") (strlen s)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
            ["std::string s = \"Hello, Martians\";"
             "s.length();"])))

;; loops (ex: while, doseq)

(defexpect loops
  (let [ast (az/analyze '(while true (println "e")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"while (true)
{
  cout << \"e\" << endl;
}")))

;; other built-in fns (also marked with op = :static-call)

(defexpect get-test
  (let [ast (az/analyze '(do (def ^{:mtype [Map [String Integer]]} numberWords {"one" 1
                                                                                "two" 2
                                                                                "three" 3})
                             (get numberWords "one")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
            ["std::map<std::string,int> numberWords;
numberWords.insert(std::make_pair(\"one\", 1));
numberWords.insert(std::make_pair(\"two\", 2));
numberWords.insert(std::make_pair(\"three\", 3));"
             "numberWords[\"one\"];"]
            )))

(defexpect nth-test
  (let [ast (az/analyze '(do (def ^{:mtype [List [Integer]]} numbers [13 17 19 23])
                             (nth numbers 2)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
            ["std::vector<int> numbers = {13, 17, 19, 23};"
             "numbers[2];"]
            )))

;; not

(defexpect not-test
  (let [ast (az/analyze '(not (= 3 (/ 10 2))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
            "!(3 == (10 / 2))")))

;; new

(defexpect new-test
  (let [ast (az/analyze '(StringBuffer.))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
            "StringBuffer"))
  (let [ast (az/analyze '(StringBuffer. "Initial string value"))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
            "StringBuffer(\"Initial string value\")")))

;; string buffer - new

(defexpect stringbuffer-new
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
}")))

;; string buffer - insert

(defexpect stringbuffer-insert-char
  (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] (insert-strbuf-char sb 0 \x)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  std::string sb = \"\";
  sb.insert(0, 'x');
}")))

(defexpect stringbuffer-insert-string
  (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] (insert-strbuf-string sb 0 "hello")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  std::string sb = \"\";
  sb.insert(0, \"hello\");
}")))

;; string buffer - length

(defexpect stringbuffer-length
  (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))]
                           (insert-strbuf-string sb 0 "hello")
                           (length-strbuf sb)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  std::string sb = \"\";
  sb.insert(0, \"hello\");
  sb.length();
}")))

;; string buffer - prepend

(defexpect stringbuffer-prepend
  (let [ast (az/analyze '(let [^StringBuffer sb (atom (new-strbuf))] (prepend-strbuf sb "hello")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"{
  std::string sb = \"\";
  sb = \"hello\" + sb;
}")))

;; string - equals

(defexpect str-eq-test
  (let [ast (az/analyze '(do (def ^String s1 "house")
                             (def ^String s2 "home")
                             (str-eq s1 s2)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
["std::string s1 = \"house\";"
 "std::string s2 = \"home\";"
 "s1 == s2;"])))

;; sequential collection - length

(defexpect seq-length-test
  (let [ast (az/analyze '(do (def ^{:mtype [List [Character]]} formattedDigits []) (seq-length formattedDigits)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
["std::vector<char16_t> formattedDigits = {};"
 "formattedDigits.size();"])))

;; sequential collection - append

(defexpect seq-append-test
  (let [ast (az/analyze '(do (def ^{:mtype [List [Character]]} formattedDigits []) (seq-append formattedDigits \1)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
["std::vector<char16_t> formattedDigits = {};"
 "formattedDigits.push_back('1');"])))

;; demo code

(defexpect demo
  (let [ast (az/analyze '(defclass "NumFmt"
                           (defn format ^String [^Integer num]
                             (let [^Integer i (atom num)
                                   ^StringBuffer result (atom (new-strbuf))]
                               (while (not (= @i 0))
                                 (let [^Integer quotient (quot @i 10)
                                       ^Integer remainder (rem @i 10)]
                                   (prepend-strbuf @result (str remainder))
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
                                   (prepend-strbuf @result (str remainder))
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
};")))

(defexpect contains-test
  (let [ast (az/analyze '(do
                           (def ^{:mtype [Map [String Integer]]} numberWords {"one" 1})
                           (contains? numberWords "ten")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
["std::map<std::string,int> numberWords;
numberWords.insert(std::make_pair(\"one\", 1));"
 "numberWords.count(\"ten\") > 0;"])))

(defexpect invoke-test
  (let [ast (az/analyze '(do
                           (defn f ^Integer [^Integer a1 ^Integer a2 ^Integer a3]
                             (return (+ a1 a2 a3)))
                           (f 1 2 3)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
            ["int f(int a1, int a2, int a3)
{
  return a1 + a2 + a3;
}"
             "f(1, 2, 3);"])))
