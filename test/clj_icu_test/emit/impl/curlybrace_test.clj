(ns clj-icu-test.emit.impl.curlybrace-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
  (:import clj_icu_test.common.AstOpts))

;; static call (arithmetic operations)

;; static call - +

(defexpect static-call-addition
  (let [ast (az/analyze '(+ 11 17))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "11 + 17")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "11 + 17")))    

;; static call - /

(defexpect static-call-division
  (let [ast (az/analyze '(/ 34 17))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "34 / 17")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "34 / 17")))

;; static call - =

(defexpect static-call-equals
  (let [ast (az/analyze '(= 34 (* 2 17)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "34 == (2 * 17)")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "34 == (2 * 17)")))

;; static call - convert fn names to symbols

(defexpect static-call-names-to-operators
  (let [ast (az/analyze '(quot 37 10))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "37 / 10")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "37 / 10")) 
  (let [ast (az/analyze '(rem 37 10))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "37 % 10")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "37 % 10")))

;; language - multiple operands

(defexpect lang-mult-operands
  (let [ast (az/analyze '(+ 11 17 19 23))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java})) "11 + 17 + 19 + 23")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp})) "11 + 17 + 19 + 23")))

;; if

(defexpect if-test
  (let [ast (az/analyze '(if (== 8 (+ 5 3))
                           (println "equals!")
                           (println "not equals.")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
"if (8 == (5 + 3))
{
  System.out.println(\"\" + \"equals!\");
}
else
{
  System.out.println(\"\" + \"not equals.\");
}"))
  (let [ast (az/analyze '(if (== 8 (+ 5 3))
                           (println "equals!")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
"if (8 == (5 + 3))
{
  System.out.println(\"\" + \"equals!\");
}")))

(defexpect cond-test
  (let [ast (az/analyze '(cond
                           (= 1 3) (println "3")
                           (= 1 11) (println "11")
                           :else (println "hmm")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
"if (1 == 3)
{
  System.out.println(\"\" + \"3\");
}
else if (1 == 11)
{
  System.out.println(\"\" + \"11\");
}
else
{
  System.out.println(\"\" + \"hmm\");
}"))
  (let [ast (az/analyze '(cond
                           (= 1 3) (println "3")
                           (= 1 11) (println "11")))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
"if (1 == 3)
{
  System.out.println(\"\" + \"3\");
}
else if (1 == 11)
{
  System.out.println(\"\" + \"11\");
}")))

;; nil

(defexpect nil-test
  (let [ast (az/analyze 'nil)]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
            "null"))
  (let [ast (az/analyze '(do (def x 3) (not= x nil)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
            ["x = 3;"
             "x != null;"])))

;; not=

(defexpect not=-test
  (let [ast (az/analyze '(not= 3 (/ 10 2)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
            "3 != 10 / 2")))

;; args

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

;; loops

(defexpect emit-dotimes-test
  (let [ast (az/analyze '(dotimes [^Integer i 10]
                           (println i)))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
            "for(Integer i = 0; i < 10; i++)
{
  System.out.println(\"\" + i);
}")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
            "for(int i = 0; i < 10; i++)
{
  cout << i << endl;
}")))

;; ns

(defexpect emit-ns-form
  (let [ast (az/analyze '(ns clj-icu-test.demo.demo02
                           (:require [clj-icu-test.common :refer :all])))
        ast-opts (map->AstOpts {:ast ast :lang ::l/java})]
    (expect (emit ast-opts) nil)))

;; metadata

(defexpect emit-with-meta-test
  (import 'java.util.List)
  (let [ast (az/analyze '(do ^{:mtype [List [Character]]} [1 2 3]))
        ast-opts (map->AstOpts {:ast ast :lang ::l/java})] 
    (expect (emit ast-opts)
            "Arrays.asList(1, 2, 3)"))
  (let [ast (az/analyze '(do ^{:mtype [List [Character]] :hello "world"} [1 2 3]))
        ast-opts (map->AstOpts {:ast ast :lang ::l/java})]
    (expect (emit ast-opts)
            "Arrays.asList(1, 2, 3)")))

;; return statement

(defexpect return-test
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
}")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
"public Integer add(Integer x, Integer y)
{
  {
    Integer sum = x + y;
    return sum;
  }
}")) 
  (let [ast (az/analyze '(defn add ^Integer [^Integer x ^Integer y]
                           (return (+ x y))))]
    (expect (emit (map->AstOpts {:ast ast :lang ::l/cpp}))
"int add(int x, int y)
{
  return x + y;
}")
    (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
            "public Integer add(Integer x, Integer y)
{
  return x + y;
}")))
