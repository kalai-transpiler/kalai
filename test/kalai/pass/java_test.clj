(ns kalai.pass.java-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.pass.test-helpers :refer [ns-form top-level-form inner-form]]))

;; # Creating Variables

(deftest init1-test
  (top-level-form
    '(def ^{:t "int"} x 3)
    ;;->
    '(init x 3)
    ;;->
    "final int x = 3;"))

(deftest init2-test
    (top-level-form
      '(def ^Integer x)
      ;;->
      '(init x)
      ;;->
      "final Integer x;"))

(deftest init3-test
  (inner-form
    '(let [^int x 1]
       x)
    ;;->
    '(do
       (init x 1)
       x)
    ;;->
    "{
final int x = 1;
x;
}"))

;; # Functions

(deftest function-test
  (testing
    "In Kalai, you repesent a function just how you would in Clojure"
    (top-level-form
      '(defn f ^Integer [^Integer x]
         (inc x))
      ;;->
      '(function f [x]
                 (return (operator ++ x)))
      ;;->
      "public static final Integer f(final Integer x) {
return ++x;
}")))

(deftest function2-test
  (testing
    "Some Clojure forms expand to multiple statements.
    The way Kalai deals with this is by..."

    "A function with no argument and a mutable local variable,
    and returning a Clojure form that expands to multiple statements."
    (top-level-form
      '(defn f ^int []
         (let [^int x (atom 0)]
           (swap! x inc)))
      ;;->
      '(function f []
                 (do
                   (init x 0)
                   (group
                     (assign x (operator ++ x))
                     (return x))))
      ;;->
      "public static final int f() {
int x = 0;
x = ++x;
return x;
}")))

;; Multiple arity aka overloaded methods
(deftest function3-test
  (ns-form
    '((ns test-package.test-class)
      (defn f
        (^int [^int x]
         (inc x))
        (^int [^int x ^int y]
         (+ x y))))
    ;;->
    '(namespace test-package.test-class
      (function f [x]
                (return (operator ++ x)))
      (function f [x y]
                (return (operator + x y))))
    ;;->
    "package test-package;
public class test-class {
public static final int f(final int x) {
return ++x;
}
public static final int f(final int x, final int y) {
return (x + y);
}
}"))

;; Custom type void does not return a value
(deftest function4-test
  (top-level-form
    '(defn f ^{:t :void} [^int x]
       (println x))
    ;;->
    '(function f [x]
               (invoke println x))
    ;;->
    "public static final void f(final int x) {
System.out.println(x);
}"))

;; # Local variables

(deftest local-variables-test
  (inner-form
    '(let [^int x (atom 0)]
       (reset! x (+ @x 2)))
    ;;->
    '(do
       (init x 0)
       (assign x (operator + x 2)))
    ;;->
    "{
int x = 0;
x = (x + 2);
}"))

(deftest local-variables2-test
  (inner-form
    '(let [^int x (atom 1)
           ^int y (atom 2)
           ^int z 1]
       (reset! x 3)
       (+ @x (deref y)))
    ;;->
    '(do
       (init x 1)
       (init y 2)
       (init z 1)
       (do
         (assign x 3)
         (operator + x y)))
    ;;->
    "{
int x = 1;
int y = 2;
final int z = 1;
{
x = 3;
(x + y);
}
}"))

(deftest local-variables3-test
  (inner-form
    '(with-local-vars [^int x 1
                       ^int y 2]
       (+ (var-get x) (var-get y)))
    ;;->
    '(do
       (init x 1)
       (init y 2)
       (operator + x y))
    ;;->
    "{
int x = 1;
int y = 2;
(x + y);
}"))

;; # Types

;; Exhibits the generosity of our type system
(deftest primitives-types-test
  (inner-form
    '(do (def ^{:t :Boolean} x true)
         (def x true)
         (def ^{:t "Long"} y 5))
    ;;->
    '(do
       (init x true)
       (init x true)
       (init y 5))
    ;;->
    "{
final Boolean x = true;
final bool x = true;
final Long y = 5;
}"))

(deftest generic-types-test
  (top-level-form
    '(do (def ^{:t {:map [:long :string]}} x))
    ;;->
    '(init x)
    ;;->
    "final Map<Long,String> x;"))

(deftest type-aliasing-test
  (ns-form
    '((ns test-package.test-class)
      (def ^{:kalias {:map [:long :string]}} T)
      (def ^{:t T} x))
    ;;->
    '(namespace test-package.test-class
                (init x))
    ;;->
    "package test-package;
public class test-class {
final Map<Long,String> x;
}"))

;; unparameterized form
(deftest generic-types2-test
  (inner-form
    '(let [^{:t kvector} x [1 2]]
       (println x))
    ;;->
    '(do
       (init x [1 2])
       (invoke println x))
    ;;->
    "{
final PersistentVector tmp1 = new PersistentVector();
tmp1.add(1);
tmp1.add(2);
final Vector x = tmp1;
System.out.println(x);
}"))

(deftest generic-types3-test
  (inner-form
    '(def ^{:t {:map [:string :string]}} x {:a "asdf"})
    ;;->
    '(init x {:a "asdf"})
    ;;->
    "final PersistentMap tmp1 = new PersistentMap();
tmp1.put(\":a\", \"asdf\");
final Map<String,String> x = tmp1;"))

;; # Conditionals

(deftest conditional-test
  (inner-form
    '(if true 1 2)
    ;;->
    '(if true 1 2)
    ;;->
    "if (true)
{
1;
}
else
{
2;
}"))

;; # Data Literals

(deftest data-literals-test
  (inner-form
    [1 2]
    ;;->
    [1 2]
    ;;->
    "final PersistentVector tmp1 = new PersistentVector();
tmp1.add(1);
tmp1.add(2);
tmp1;"))

;; selecting between Vector and PersistentVector
(deftest data-literals2-test
  (inner-form
    '(def x ^:mut [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "final Vector tmp1 = new Vector();
tmp1.add(1);
tmp1.add(2);
final Vector x = tmp1;"))

(deftest data-literals3-test
  (inner-form
    '(let [x ^:mut [1 2]]
       x)
    ;;->
    '(do
       (init x [1 2])
       x)
    ;;->
    "{
final Vector tmp1 = new Vector();
tmp1.add(1);
tmp1.add(2);
final Vector x = tmp1;
x;
}"))

(deftest data-literals4-test
  (inner-form
    '(let [x (atom ^:mut [1 2])]
       (reset! x ^:mut [3 4]))
    ;;->
    '(do
       (init x [1 2])
       (assign x [3 4]))
    ;;->
    "{
final Vector tmp1 = new Vector();
tmp1.add(1);
tmp1.add(2);
Vector x = tmp1;
final Vector tmp2 = new Vector();
tmp2.add(3);
tmp2.add(4);
x = tmp2;
}"))

(deftest data-literals5-test
  (inner-form
    {1 2 3 4}
    ;;->
    {1 2 3 4}
    ;;->
    "final PersistentMap tmp1 = new PersistentMap();
tmp1.put(1, 2);
tmp1.put(3, 4);
tmp1;"))

(deftest data-literals6-test
  (inner-form
    #{1 2}
    ;;->
    #{1 2}
    ;;->
    "final PersistentSet tmp1 = new PersistentSet();
tmp1.add(1);
tmp1.add(2);
tmp1;"))

(deftest data-literals7-test
  (inner-form
    '(let [^{:t kvector} x [1 [2]]]
       (println x))
    ;;->
    '(do
       (init x [1 [2]])
       (invoke println x))
    ;;->
    "{
final PersistentVector tmp1 = new PersistentVector();
tmp1.add(1);
final PersistentVector tmp2 = new PersistentVector();
tmp2.add(2);
tmp1.add(tmp2);
final Vector x = tmp1;
System.out.println(x);
}"))

(deftest data-literals8-test
  (inner-form
    '(let [^{:t kvector} x [1 [2] 3 [[4]]]]
       (println x))
    ;;->
    '(do
       (init x [1 [2] 3 [[4]]])
       (invoke println x))
    ;;->
    "{
final PersistentVector tmp1 = new PersistentVector();
tmp1.add(1);
final PersistentVector tmp2 = new PersistentVector();
tmp2.add(2);
tmp1.add(tmp2);
tmp1.add(3);
final PersistentVector tmp3 = new PersistentVector();
final PersistentVector tmp4 = new PersistentVector();
tmp4.add(4);
tmp3.add(tmp4);
tmp1.add(tmp3);
final Vector x = tmp1;
System.out.println(x);
}"))

(deftest data-literals9-test
  (inner-form
    '(let [^{:t kvector} x {1 [{2 3} #{4 [5 6]}]}]
       (println x))
    ;;->
    '(do
       (init x {1 [{2 3} #{4 [5 6]}]})
       (invoke println x))
    ;;->
    "{
final PersistentMap tmp1 = new PersistentMap();
final PersistentVector tmp2 = new PersistentVector();
final PersistentMap tmp3 = new PersistentMap();
tmp3.put(2, 3);
tmp2.add(tmp3);
final PersistentSet tmp4 = new PersistentSet();
tmp4.add(4);
final PersistentVector tmp5 = new PersistentVector();
tmp5.add(5);
tmp5.add(6);
tmp4.add(tmp5);
tmp2.add(tmp4);
tmp1.put(1, tmp2);
final Vector x = tmp1;
System.out.println(x);
}"))

(deftest data-literals10-test
  (inner-form
    '{"key" (+ 1 2)}
    ;;->
    '{"key" (operator
              +
              1
              2)}
    ;;->
    "final PersistentMap tmp1 = new PersistentMap();
tmp1.put(\"key\", (1 + 2));
tmp1;"))

(deftest foreach-test
  (inner-form
    '(doseq [^int x [1 2 3 4]]
       (println x))
    ;;->
    '(foreach x [1 2 3 4]
              (invoke println x))
    ;;->
    "final PersistentVector tmp1 = new PersistentVector();
tmp1.add(1);
tmp1.add(2);
tmp1.add(3);
tmp1.add(4);
for (int x : tmp1) {
System.out.println(x);
}"))

(deftest for-loop-test
  (inner-form
    '(dotimes [x 5]
       (println x))
    ;;->
    '(group
       (init x 0)
       (while (operator < x 5)
         (invoke println x)
         (assign x (operator ++ x))))
    ;;->
    "int x = 0;
while ((x < 5)) {
System.out.println(x);
x = ++x;
}"))

(deftest while-loop-test
  (inner-form
    '(while true
       (println "hi"))
    ;;->
    '(while true
       (invoke println "hi"))
    ;;->
    "while (true) {
System.out.println(\"hi\");
}"))

(deftest conditional2-test
  (inner-form
    '(cond true 1
           false 2
           :else 3)
    ;;->
    '(if true
       1
       (if false
         2
         (if :else
           3)))
    ;;->
    "if (true)
{
1;
}
else
{
if (false)
{
2;
}
else
{
if (\":else\")
{
3;
}
}
}"))

(deftest keywords-as-functions-test
  (inner-form
    '(:k {:k 1})
    ;;->
    '(method get {:k 1} :k)
    ;;->
    "final PersistentMap tmp1 = new PersistentMap();
tmp1.put(\":k\", 1);
tmp1.get(\":k\");"))

(deftest keywords-as-functions2-test
  (inner-form
    '(:k #{:k})
    ;;->
    '(method get #{:k} :k)
    ;;->
    "final PersistentSet tmp1 = new PersistentSet();
tmp1.add(\":k\");
tmp1.get(\":k\");"))

(deftest switch-case-test
  (inner-form
    '(case 1
       1 :a
       2 :b)
    ;;->
    '(case 1 {1 [1 :a]
              2 [2 :b]})
    ;;->
    "switch (1) {
case 1 : \":a\";
break;
case 2 : \":b\";
break;
}"))

(deftest interop-test
  (inner-form
    '(let [a (new String)
           b (String.)]
       (.length a)
       (. b length))
    ;;->
    '(do
       (init a (new String))
       (init b (new String))
       (do
         (method length a)
         (method length b)))
    ;;->
    "{
final String a = new String();
final String b = new String();
{
a.length();
b.length();
}
}"))

(deftest function-calls-test
  (inner-form
    '(assoc {:a 1} :b 2)
    ;;->
    '(method put {:a 1} :b 2)
    ;;->
    "final PersistentMap tmp1 = new PersistentMap();
tmp1.put(\":a\", 1);
tmp1.put(\":b\", 2);"))

(deftest function-calls2-test
  (inner-form
    '(update {:a 1} :a inc)
    ;;->
    '(group
       (init tmp1 {:a 1})
       (method put
               tmp1
               :a
               (operator ++
                         (method get tmp1 :a)))
       tmp1)
    ;;->
    "final PersistentMap tmp2 = new PersistentMap();
tmp2.put(\":a\", 1);
final clojure.lang.PersistentArrayMap tmp1 = tmp2;
tmp1.put(\":a\", ++tmp1.get(\":a\"));
tmp1;"))

(deftest conditional-expression-test
  ;; For simple expressions, a true ternary could be used instead
  ;; "((true ? 1 : 2) + (true ? (true ? 3 : 4) : 5));"
  ;; But for now we are taking the more general approach which handles expressions.
  (inner-form
    '(+ (if true 1 2)
        (if true
          (if true 3 4)
          5))
    ;;->
    '(operator +
               (if true 1 2)
               (if true
                 (if true 3 4)
                 5))
    ;;->
    "long tmp1;
if (true)
{
tmp1 = 1;
}
else
{
tmp1 = 2;
}
long tmp2;
if (true)
{
long tmp3;
if (true)
{
tmp3 = 3;
}
else
{
tmp3 = 4;
}
{
tmp2 = tmp3;
}
}
else
{
tmp2 = 5;
}
(tmp1 + tmp2);"))

(deftest nested-group-test
  #_(inner-form
      '(+ 1 (swap! x inc))
      ;;->
      '(operator +
                 1
                 (group (assign x (invoke inc x))
                        x))
      ;;->
      "int x = (x + 1);
  (1 + x);"))

(deftest conditional-expression2-test
  (inner-form
    '(+ (if true
          (do (println 1)
              2))
        4)
    ;;->
    '(operator +
               (if true
                 (do
                   (invoke println 1)
                   2))
               4)
    ;;->
    "long tmp1;
if (true)
{
System.out.println(1);
{
tmp1 = 2;
}
}
(tmp1 + 4);"))


(deftest conditional-expression3-test
  (inner-form
    '(+ (if true 1 (if false 2 3)) 4)
    ;;->
    '(operator +
               (if true 1 (if false 2 3))
               4)
    ;;->

    "long tmp1;
if (true)
{
tmp1 = 1;
}
else
{
long tmp2;
if (false)
{
tmp2 = 2;
}
else
{
tmp2 = 3;
}
{
tmp1 = tmp2;
}
}
(tmp1 + 4);"))

(deftest conditional-expression4-test
  (inner-form
    '(+ (if true 1 (if false 2 [3])) 4)
    ;;->
    '(operator +
               (if true 1 (if false 2 [3]))
               4)
    ;;->
    "long tmp1;
if (true)
{
tmp1 = 1;
}
else
{
long tmp2;
if (false)
{
tmp2 = 2;
}
else
{
final PersistentVector tmp3 = new PersistentVector();
tmp3.add(3);
{
tmp2 = tmp3;
}
}
{
tmp1 = tmp2;
}
}
(tmp1 + 4);"))

(deftest operator-test
  (inner-form
    '(not (= 1 (inc 1)))
    ;;->
    '(operator ! (operator == 1 (operator ++ 1)))
    ;;->
    "!(1 == ++1);"))
