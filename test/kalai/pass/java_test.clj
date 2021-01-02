(ns kalai.pass.java-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.pass.test-helpers :refer [ns-form top-level-form inner-form]]))

;; # Creating Variables

(deftest init1-test
  (top-level-form
    '(def ^{:t :int} x 3)
    ;;->
    '(init x 3)
    ;;->
    "static final int x = 3;"))

(deftest init2-test
  (top-level-form
    '(def ^Integer x)
    ;;->
    '(init x)
    ;;->
    "static final int x;"))

(deftest init3-test
  (inner-form
    '(let [^{:t :int} x 1]
       x)
    ;;->
    '(do
       (init x 1)
       x)
    ;;->
    "final int x = 1;"))

(deftest init4-test
  (inner-form
    '(let [^Integer x (Integer. 1)]
       x)
    ;;->
    '(do
       (init x (new Integer 1))
       x)
    ;;->
    "final int x = new Integer(1);"))

;; TODO: def data literal in top level form
(deftest init5-test
  #_(top-level-form
      '(def x [1 2 3])
      ;;->
      '(init x [1 2 3])
      ;;->
      "static final PVector<Object> x;
  static {
  final PVector<Object> tmp1 = new PVector<Object>();
  tmp1.add(1);
  tmp1.add(2);
  tmp1.add(3);
  x = tmp1;
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
                 (return (operator + x 1)))
      ;;->
      "public static final int f(final int x) {
return (x + 1);
}")))

;; TODO: differentiate between mutable swap and not
(deftest function2-test
  #_(testing
      "Some Clojure forms expand to multiple statements.
      The way Kalai deals with this is by creating a group.
      That group is later unrolled as temporary variable assignments."
      (top-level-form
        '(defn f ^{:t :int} []
           (let [x (atom (int 0))]
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
        (^{:t :int} [^{:t :int} x]
         (inc x))
        (^{:t :int} [^{:t :int} x ^{:t :int} y]
         (+ x y))))
    ;;->
    '(namespace test-package.test-class
                (function f [x]
                          (return (operator + x 1)))
                (function f [x y]
                          (return (operator + x y))))
    ;;->
    "package testpackage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class TestClass {
public static final int f(final int x) {
return (x + 1);
}
public static final int f(final int x, final int y) {
return (x + y);
}
}"))

;; Custom type void does not return a value
(deftest function4-test
  (top-level-form
    '(defn f ^{:t :void} [^long x]
       (println x))
    ;;->
    '(function f [x]
               (invoke println x))
    ;;->
    "public static final void f(final long x) {
System.out.println(x);
}"))

;; # Local variables

(deftest local-variables-test
  (inner-form
    '(let [x (atom (int 0))]
       (reset! x (+ @x 2)))
    ;;->
    '(do
       (init x 0)
       (assign x (operator + x 2)))
    ;;->
    "int x = 0;
x = (x + 2);"))

(deftest local-variables2-test
  (inner-form
    '(let [x (atom (int 1))
           y (atom (int 2))
           z (int 1)]
       (reset! x (int 3))
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
    "int x = 1;
int y = 2;
final int z = 1;
{
x = 3;
}"))

(deftest local-variables3-test
  (inner-form
    '(with-local-vars [^int x 1
                       ^int y 2]
       (println (+ (var-get x) (var-get y))))
    ;;->
    '(do (init x 1)
         (init y 2)
         (invoke println (operator + x y)))
    ;;->
    "int x = 1;
int y = 2;
System.out.println((x + y));"))

;; TODO: mutable swap
(deftest local-variables4-test
  #_(inner-form
      '(let [y (atom 2)]
         (swap! y + 4))
      ;;->
      '(do
         (init y 2)
         (group
           (assign y (operator + y 4))
           y))
      ;;->
      "long y = 2;
  y = (y + 4);"))

(deftest local-variables5-test
  (inner-form
    '(let [^:mut ^{:t :long} y (atom (- 2 4))]
       (swap! y + 4))
    ;;->
    '(do
       (init y (operator - 2 4))
       (group
         (assign y (operator + y 4))
         y))
    ;;->
    "long y = (2 - 4);
y = (y + 4);"))

;; # Types

;; Exhibits the generosity of our type system
(deftest primitives-types-test
  (inner-form
    '(do (def ^{:t :bool} x true)
         (def x true)
         (let [z true]
           z)
         (def ^{:t :long} y 5))
    ;;->
    '(do
       (init x true)
       (init x true)
       (do
         (init z true)
         z)
       (init y 5))
    ;;->
    "final boolean x = true;
final boolean x = true;
{
final boolean z = true;
}
final long y = 5;"))

(deftest type-aliasing-test
  (ns-form
    '((ns test-package.test-class)
      (def ^{:kalias {:mmap [:long :string]}} T)
      (def ^{:t T} x)
      (defn f ^{:t T} [^{:t T} y]
        (let [^{:t T} z y]
          z)))
    ;;->
    '(namespace test-package.test-class
                (init x)
                (function f [y]
                          (do
                            (init z y)
                            (return z))))
    ;;->
    "package testpackage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
public class TestClass {
static final HashMap<Long,String> x;
public static final HashMap<Long,String> f(final HashMap<Long,String> y) {
final HashMap<Long,String> z = y;
return z;
}
}"))

(deftest generic-types-test
  (top-level-form
    '(def ^{:t {:mmap [:long :string]}} x)
    ;;->
    '(init x)
    ;;->
    "static final HashMap<Long,String> x;"))

(deftest generic-types2-test
  (top-level-form
    '(def ^{:t {:mmap [:string {:mvector [:char]}]}} x)
    ;;->
    '(init x)
    ;;->
    "static final HashMap<String,ArrayList<Character>> x;"))

(deftest generic-types3-test
  (inner-form
    '(let [x ^{:t {:mvector [:long]}} [1 2]]
       (println x))
    ;;->
    '(do
       (init x [1 2])
       (invoke println x))
    ;;->
    "final ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1);
tmp1.add(2);
final ArrayList<Long> x = tmp1;
System.out.println(x);"))

(deftest generic-types4-test
  (inner-form
    '(def ^{:t {:mmap [:string :string]}} x {:a "asdf"})
    ;;->
    '(init x {:a "asdf"})
    ;;->
    "final HashMap<String,String> tmp1 = new HashMap<String,String>();
tmp1.put(\":a\", \"asdf\");
final HashMap<String,String> x = tmp1;"))

(deftest generic-types5-test
  #_(inner-form
      '(let [x ^{:t {:array [:string]}} ["arg1" "arg2"]]
         (println x))
      ;;->
      '(do
         (init x ["arg1" "arg2"])
         (invoke println x))
      ;;->
      "final ArrayList<Long> tmp1 = new ArrayList<Long>();
  tmp1.add(1);
  tmp1.add(2);
  final ArrayList<Long> x = tmp1;
  System.out.println(x);"))

;; # Main entry point

(deftest main-method-test
  (top-level-form
    ;; return type of `void` for `main()` is implied
    '(defn -main ^{:t :void} [& args]
       1)
    ;;->
    '(function -main [& args] 1)
    ;;->
    "public static final void main(String[] args) {

}"))

;; # Conditionals

(deftest conditional-test
  (inner-form
    '(if true
       (println 1)
       (println 2))
    ;;->
    '(if true
       (invoke println 1)
       (invoke println 2))
    ;;->
    "if (true)
{
System.out.println(1);
}
else
{
System.out.println(2);
}"))

;; # Data Literals

(deftest data-literals-test
  (inner-form
    '(def ^{:t {:vector [:long]}} x [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "final PVector<Long> tmp1 = new PVector<Long>();
tmp1.add(1);
tmp1.add(2);
final PVector<Long> x = tmp1;"))

;; selecting between Vector and PVector<Object>
(deftest data-literals2-test
  (inner-form
    '(def x ^{:t {:mvector [:long]}} [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "final ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1);
tmp1.add(2);
final ArrayList<Long> x = tmp1;"))

(deftest data-literals3-test
  (inner-form
    '(let [^{:t {:mvector [:long]}} x [1 2]]
       x)
    ;;->
    '(do
       (init x [1 2])
       x)
    ;;->
    "final ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1);
tmp1.add(2);
final ArrayList<Long> x = tmp1;"))

(deftest data-literals4-test
  (inner-form
    '(let [x (atom ^{:t {:vector [:long]}} [1 2])]
       (reset! x ^{:t {:vector [:long]}} [3 4]))
    ;;->
    '(do
       (init x [1 2])
       (assign x [3 4]))
    ;;->
    "final PVector<Long> tmp1 = new PVector<Long>();
tmp1.add(1);
tmp1.add(2);
PVector<Long> x = tmp1;
final PVector<Long> tmp2 = new PVector<Long>();
tmp2.add(3);
tmp2.add(4);
x = tmp2;"))

(deftest data-literals5-test
  (inner-form
    '(def x ^{:t {:map [:long :long]}} {1 2 3 4})
    ;;->
    '(init x {1 2 3 4})
    ;;->
    "final PMap<Long,Long> tmp1 = new PMap<Long,Long>();
tmp1.put(1, 2);
tmp1.put(3, 4);
final PMap<Long,Long> x = tmp1;"))

(deftest data-literals6-test
  (inner-form
    '(def x ^{:t {:set [:long]}} #{1 2})
    ;;->
    '(init x #{1 2})
    ;;->
    "final PSet<Long> tmp1 = new PSet<Long>();
tmp1.add(1);
tmp1.add(2);
final PSet<Long> x = tmp1;"))

;; TODO: What about heterogeneous collections,
;; do we want to allow them? [1 [2]] if so what is the type?
;; Do all languages have an "Object" concept?
(deftest data-literals7-test
  (inner-form
    '(let [^{:t {:vector [{:vector [:long]}]}} x
           [[1] [2]]]
       (println x))
    ;;->
    '(do
       (init x [[1] [2]])
       (invoke println x))
    ;;->
    "final PVector<PVector<Long>> tmp1 = new PVector<PVector<Long>>();
final PVector<Long> tmp2 = new PVector<Long>();
tmp2.add(1);
tmp1.add(tmp2);
final PVector<Long> tmp3 = new PVector<Long>();
tmp3.add(2);
tmp1.add(tmp3);
final PVector<PVector<Long>> x = tmp1;
System.out.println(x);"))

(deftest data-literals7-0-test
  #_(top-level-form
      '(defn f []
         (let [x ^{:t {:vector [{:vector [:long]}]}}
                 [[1] [2]]]
           x)
         ^{:t {:vector [{:vector [:long]}]}}
         [[1] [2]])
      ;;->
      '(function f [] (return [[1] [2]]))
      ;;->
      "final PVector<PVector<Long>> tmp1 = new PVector<PVector<Long>>();
  final PVector<Long> tmp2 = new PVector<Long>();
  tmp2.add(1);
  tmp1.add(tmp2);
  final PVector<Long> tmp3 = new PVector<Long>();
  tmp3.add(2);
  tmp1.add(tmp3);
  final PVector<PVector<Long>> x = tmp1;
  System.out.println(x);"))

(deftest data-literals7-1-test
  (inner-form
    '(let [^{:t {:mmap [:long {:mvector [:string]}]}} x
           {1 ["hi"]
            2 ["hello" "there"]}]
       (println x))
    ;;->
    '(do
       (init x {1 ["hi"]
                2 ["hello" "there"]})
       (invoke println x))
    ;;->
    "final HashMap<Long,ArrayList<String>> tmp1 = new HashMap<Long,ArrayList<String>>();
final ArrayList<String> tmp2 = new ArrayList<String>();
tmp2.add(\"hi\");
tmp1.put(1, tmp2);
final ArrayList<String> tmp3 = new ArrayList<String>();
tmp3.add(\"hello\");
tmp3.add(\"there\");
tmp1.put(2, tmp3);
final HashMap<Long,ArrayList<String>> x = tmp1;
System.out.println(x);"))

(deftest data-literals7-2-test
  (inner-form
    '(let [^{:t {:vector [{:map [{:set [:long]} {:vector [:string]}]}]}} x
           [{#{1} ["hi"]
             #{2} ["hello" "there"]}]]
       (println x))
    ;;->
    '(do
       (init x [{#{1} ["hi"]
                 #{2} ["hello" "there"]}])
       (invoke println x))
    ;;->
    "final PVector<PMap<PSet<Long>,PVector<String>>> tmp1 = new PVector<PMap<PSet<Long>,PVector<String>>>();
final PMap<PSet<Long>,PVector<String>> tmp2 = new PMap<PSet<Long>,PVector<String>>();
final PSet<Long> tmp3 = new PSet<Long>();
tmp3.add(1);
final PVector<String> tmp4 = new PVector<String>();
tmp4.add(\"hi\");
tmp2.put(tmp3, tmp4);
final PSet<Long> tmp5 = new PSet<Long>();
tmp5.add(2);
final PVector<String> tmp6 = new PVector<String>();
tmp6.add(\"hello\");
tmp6.add(\"there\");
tmp2.put(tmp5, tmp6);
tmp1.add(tmp2);
final PVector<PMap<PSet<Long>,PVector<String>>> x = tmp1;
System.out.println(x);"))

(deftest data-literals7-3-test
  (inner-form
    '(let [x
           ^{:t {:vector [{:map [{:set [:long]} {:vector [:string]}]}]}}
           [{#{1} ["hi"]
             #{2} ["hello" "there"]}]]
       (println x))
    ;;->
    '(do
       (init x [{#{1} ["hi"]
                 #{2} ["hello" "there"]}])
       (invoke println x))
    ;;->
    "final PVector<PMap<PSet<Long>,PVector<String>>> tmp1 = new PVector<PMap<PSet<Long>,PVector<String>>>();
final PMap<PSet<Long>,PVector<String>> tmp2 = new PMap<PSet<Long>,PVector<String>>();
final PSet<Long> tmp3 = new PSet<Long>();
tmp3.add(1);
final PVector<String> tmp4 = new PVector<String>();
tmp4.add(\"hi\");
tmp2.put(tmp3, tmp4);
final PSet<Long> tmp5 = new PSet<Long>();
tmp5.add(2);
final PVector<String> tmp6 = new PVector<String>();
tmp6.add(\"hello\");
tmp6.add(\"there\");
tmp2.put(tmp5, tmp6);
tmp1.add(tmp2);
final PVector<PMap<PSet<Long>,PVector<String>>> x = tmp1;
System.out.println(x);"))

(deftest data-literals8-test
  (inner-form
    '(let [x ^{:t {:mvector [:any]}}
             [1
              ^{:t {:mvector [:long]}} [2]
              3
              ^{:t {:mvector [:any]}} [^{:t {:mvector [:long]}} [4]]]]
       (println x))
    ;;->
    '(do
       (init x [1 [2] 3 [[4]]])
       (invoke println x))
    ;;->
    "final ArrayList<Object> tmp1 = new ArrayList<Object>();
tmp1.add(1);
final ArrayList<Long> tmp2 = new ArrayList<Long>();
tmp2.add(2);
tmp1.add(tmp2);
tmp1.add(3);
final ArrayList<Object> tmp3 = new ArrayList<Object>();
final ArrayList<Long> tmp4 = new ArrayList<Long>();
tmp4.add(4);
tmp3.add(tmp4);
tmp1.add(tmp3);
final ArrayList<Object> x = tmp1;
System.out.println(x);"))

(deftest data-literals9-test
  (inner-form
    '(let [^{:t {:mmap [:any :any]}} x
           {1 ^{:t {:mvector [:any]}} [^{:t {:mmap [:long :long]}} {2 3}
                                       ^{:t {:mset [:any]}} #{4
                                                              ^{:t {:mvector [:long]}} [5 6]}]}]
       (println x))
    ;;->
    '(do
       (init x {1 [{2 3} #{4 [5 6]}]})
       (invoke println x))
    ;;->
    "final HashMap<Object,Object> tmp1 = new HashMap<Object,Object>();
final ArrayList<Object> tmp2 = new ArrayList<Object>();
final HashMap<Long,Long> tmp3 = new HashMap<Long,Long>();
tmp3.put(2, 3);
tmp2.add(tmp3);
final HashSet<Object> tmp4 = new HashSet<Object>();
tmp4.add(4);
final ArrayList<Long> tmp5 = new ArrayList<Long>();
tmp5.add(5);
tmp5.add(6);
tmp4.add(tmp5);
tmp2.add(tmp4);
tmp1.put(1, tmp2);
final HashMap<Object,Object> x = tmp1;
System.out.println(x);"))

(deftest data-literals10-test
  (inner-form
    '(def ^{:t {:mmap [:string :long]}} x {"key" (+ 1 2)})
    ;;->
    '(init x {"key" (operator + 1 2)})
    ;;->
    "final HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(\"key\", (1 + 2));
final HashMap<String,Long> x = tmp1;"))

(deftest string-equality-test
  (inner-form
    '(println (= "abc" "abc"))
    ;;->
    '(invoke println (operator == "abc" "abc"))
    ;;->
    "System.out.println(\"abc\".equals(\"abc\"));"))

(deftest string-equality2-test
  (inner-form
    '(let [x "abc"
           y "abc"]
       (println (= x y)))
    ;;->
    '(do
       (init x "abc")
       (init y "abc")
       (invoke println (operator == x y)))
    ;;->
    "final String x = \"abc\";
final String y = \"abc\";
System.out.println(x.equals(y));"))

(deftest string-equality2-test
  (inner-form
    '(let [^{:t :string} x (String. "abc")
           y "abc"]
       (println (= x y)))
    ;;->
    '(do
       (init x (new
                 String
                 "abc"))
       (init y "abc")
       (invoke println (operator == x y)))
    ;;->
    "final String x = new String(\"abc\");
final String y = \"abc\";
System.out.println(x.equals(y));"))

(deftest foreach-test
  (inner-form
    '(doseq [^int x ^{:t {:mvector [:long]}} [1 2 3 4]]
       (println x))
    ;;->
    '(foreach x [1 2 3 4]
              (invoke println x))
    ;;->
    "final ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1);
tmp1.add(2);
tmp1.add(3);
tmp1.add(4);
for (int x : tmp1) {
System.out.println(x);
}"))

(deftest for-loop-test
  (inner-form
    '(dotimes [^{:t :int} x 5]
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
    '(cond true (println 1)
           false (println 2)
           true (println 3))
    ;;->
    '(if true
       (invoke println 1)
       (if false
         (invoke println 2)
         (if true
           (invoke println 3))))
    ;;->
    "if (true)
{
System.out.println(1);
}
else
{
if (false)
{
System.out.println(2);
}
else
{
if (true)
{
System.out.println(3);
}
}
}"))

(deftest keywords-as-functions-test
  (inner-form
    '(:k ^{:t {:mmap [:string :long]}} {:k 1})
    ;;->
    '(method get {:k 1} :k)
    ;;->
    "final HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(\":k\", 1);
tmp1.get(\":k\");"))

(deftest keywords-as-functions2-test
  (inner-form
    '(:k ^{:t {:mset [:string]}} #{:k})
    ;;->
    '(method get #{:k} :k)
    ;;->
    "final HashSet<String> tmp1 = new HashSet<String>();
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
    "final String a = new String();
final String b = new String();
{
a.length();
b.length();
}"))

(deftest function-calls-test
  (inner-form
    '(assoc ^{:t {:mmap [:string :long]}} {:a 1} :b 2)
    ;;->
    '(method put {:a 1} :b 2)
    ;;->
    "final HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(\":a\", 1);
tmp1.put(\":b\", 2);"))

(deftest function-calls2-test
  (inner-form
    '(update ^{:t {:mmap [:string :long]}} {:a 1} :a inc)
    ;;->
    '(group
       (init tmp1 {:a 1})
       (method put
               tmp1
               :a
               (operator + (method get tmp1 :a) 1))
       tmp1)
    ;;->
    "final HashMap<String,Long> tmp2 = new HashMap<String,Long>();
tmp2.put(\":a\", 1);
final HashMap<String,Long> tmp1 = tmp2;
tmp1.put(\":a\", (tmp1.get(\":a\") + 1));"))

(deftest conditional-expression-test
  ;; For simple expressions, a true ternary could be used instead
  ;; "((true ? 1 : 2) + (true ? (true ? 3 : 4) : 5));"
  ;; But for now we are taking the more general approach which handles expressions.
  (inner-form
    '(println
       (+ (if true 1 2)
          (if true
            (if true 3 4)
            5)))
    ;;->
    '(invoke println
             (operator +
                       (if true 1 2)
                       (if true
                         (if true 3 4)
                         5)))
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
System.out.println((tmp1 + tmp2));"))

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
    '(println
       (+ (if true
            (do (println 1)
                2))
          4))
    ;;->
    '(invoke println
             (operator +
                       (if true
                         (do
                           (invoke println 1)
                           2))
                       4))
    ;;->
    "long tmp1;
if (true)
{
System.out.println(1);
{
tmp1 = 2;
}
}
System.out.println((tmp1 + 4));"))


(deftest conditional-expression3-test
  (inner-form
    '(println
       (+ (if true 1 (if false 2 3)) 4))
    ;;->
    '(invoke println
             (operator +
                       (if true 1 (if false 2 3))
                       4))
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
System.out.println((tmp1 + 4));"))

(deftest conditional-expression4-test
  (inner-form
    '(println
       (+ (if true 1 (if false 2 ^{:t {:vector [:long]}} [3])) 4))
    ;;->
    '(invoke println
             (operator +
                       (if true 1 (if false 2 [3]))
                       4))
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
final PVector<Long> tmp3 = new PVector<Long>();
tmp3.add(3);
{
tmp2 = tmp3;
}
}
{
tmp1 = tmp2;
}
}
System.out.println((tmp1 + 4));"))

(deftest operator-test
  (inner-form
    '(println
       (not (= 1 (inc 1))))
    ;;->
    '(invoke println
             (operator ! (operator == 1 (operator + 1 1))))
    ;;->
    "System.out.println(!(1 == (1 + 1)));"))

(deftest zzz-test
  (inner-form
    '(def ^{:t :int} x (count "abc"))
    ;;->
    '(init x (invoke clojure.lang.RT/count "abc"))
    ;;->
    "final int x = \"abc\".length();"))

(deftest zzz2-test
  (inner-form
    '(let [^String s "abc"]
       (println (nth s 1)))
    ;;->
    '(do
       (init s "abc")
       (invoke println
               (invoke clojure.lang.RT/nth s 1)))
    ;;->
    "final String s = \"abc\";
System.out.println(s.charAt(1));"))

(deftest zzz3-test
  (inner-form
    '(let [^{:t {:mvector [:int]}} v [1 2 3]]
       (println (nth v 1)))
    ;;->
    '(do
       (init v [1 2 3])
       (invoke println
               (invoke clojure.lang.RT/nth v 1)))
    ;;->
    "final ArrayList<Integer> tmp1 = new ArrayList<Integer>();
tmp1.add(1);
tmp1.add(2);
tmp1.add(3);
final ArrayList<Integer> v = tmp1;
System.out.println(v.get(1));"))

(deftest yyy-test
  (inner-form
    '(let [result (atom ^{:t {:mvector [:int]}} [])
           i (atom (int 10))]
       (while (< 0 @i)
         (swap! result conj @i)
         (reset! i (- @i 3))))
    ;;->
    '(do
       (init result [])
       (init i 10)
       (while (operator < 0 i)
         (method add result i)
         (assign i (operator - i 3))))
    ;;->
    "final ArrayList<Integer> tmp1 = new ArrayList<Integer>();
ArrayList<Integer> result = tmp1;
int i = 10;
while ((0 < i)) {
result.add(i);
i = (i - 3);
}"))

(deftest yyy2-test
  (inner-form
    '(let [^{:t {:mvector [:int]}} separatorPositions nil
           ^{:t :int} numPositions (.size separatorPositions)]
       (println "hi"))
    ;;->
    '(do
       (init separatorPositions nil)
       (init numPositions
             (method size separatorPositions))
       (invoke println "hi"))
    ;;->
    "final ArrayList<Integer> separatorPositions = null;
final int numPositions = separatorPositions.size();
System.out.println(\"hi\");"))

(deftest return-if-test
  (top-level-form
    '(defn f ^{:t :long} []
       (if true
         1
         (let [x 2]
           (println "hi")
           x)))
    ;;->
    '(function f []
               (if true
                 (return 1)
                 (do
                   (init x 2)
                   (do
                     (invoke println "hi")
                     (return x)))))
    ;;->
    "public static final long f() {
if (true)
{
return 1;
}
else
{
final long x = 2;
{
System.out.println(\"hi\");
return x;
}
}
}"))

(deftest propagated-types-test
  (inner-form
    '(let [x 1
           y x]
       (println y))
    ;;->
    '(do
       (init x 1)
       (init y x)
       (invoke println y))
    ;;->
    "final long x = 1;
final long y = x;
System.out.println(y);"))

(deftest propagated-types2-test
  (inner-form
    '(let [^{:t :int} x 1
           y x]
       (println y))
    ;;->
    '(do
       (init x 1)
       (init y x)
       (invoke println y))
    ;;->
    "final int x = 1;
final int y = x;
System.out.println(y);"))

(deftest propagated-types2-1-test
  (inner-form
    '(let [x 1
           y x
           z y]
       (println z))
    ;;->
    '(do
       (init x 1)
       (init y x)
       (init z y)
       (invoke println z))
    ;;->
    "final long x = 1;
final long y = x;
final long z = y;
System.out.println(z);"))

(deftest propagated-types2-2-test
  (inner-form
    '(let [x ^{:t {:mvector [:int]}} []
           y x
           z y]
       (println z))
    ;;->
    '(do
       (init x [])
       (init y x)
       (init z y)
       (invoke println z))
    ;;->
    "final ArrayList<Integer> tmp1 = new ArrayList<Integer>();
final ArrayList<Integer> x = tmp1;
final ArrayList<Integer> y = x;
final ArrayList<Integer> z = y;
System.out.println(z);"))

(deftest propagated-types3-test
  (inner-form
    '(let [a (atom 1)
           ;; TODO: shouldn't have to declare the type of x here
           ^{:t :long} x (cond
                           true @a
                           false @a)]
       (println x))
    ;;->
    '(do
       (init a 1)
       (init x (if true
                 a
                 (if false
                   a)))
       (invoke println x))
    ;;->
    "long a = 1;
long tmp1;
if (true)
{
tmp1 = a;
}
else
{
long tmp2;
if (false)
{
tmp2 = a;
}
{
tmp1 = tmp2;
}
}
final long x = tmp1;
System.out.println(x);"))

(deftest propagated-types4-test
  (inner-form
    ;; TODO: it would be nice to be able to type infer from x to the inner vectors
    '(let [x (atom ^{:t {:mvector [:long]}} [])]
       (reset! x ^{:t {:mvector [:long]}} [1 2 3]))
    ;;->
    '(do
       (init x [])
       (assign x [1 2 3]))
    ;;->
    "final ArrayList<Long> tmp1 = new ArrayList<Long>();
ArrayList<Long> x = tmp1;
final ArrayList<Long> tmp2 = new ArrayList<Long>();
tmp2.add(1);
tmp2.add(2);
tmp2.add(3);
x = tmp2;"))

(deftest propagated-types5-test
  (inner-form
    ;; TODO: it would be nice to annotate x here instead
    '(let [x (atom ^{:t {:mvector [:long]}} [])]
       (swap! x conj 1))
    ;;->
    '(do
       (init x [])
       (method add x 1))
    ;;->
    "final ArrayList<Long> tmp1 = new ArrayList<Long>();
ArrayList<Long> x = tmp1;
x.add(1);"))

(deftest propagated-types6-test
  (top-level-form
    '(defn f ^{:t :void} [^String s]
       ;; TODO: type of x should be inferred from s
       (let [^String x s]
         (println x)))
    ;;->
    '(function f [s]
               (do
                 (init x s)
                 (invoke println x)))
    ;;->
    "public static final void f(final String s) {
final String x = s;
System.out.println(x);
}"))

(deftest propagated-types7-test
  ;; TODO: we should propagate from the arglist return type to the return expression
  #_(top-level-form
      '(defn f ^{:t {:vector [:int]}} []
         [1])
      ;;->
      '(function f []
                 (return [1]))
      ;;->
      "public static final ArrayList<Integer> f() {
  final ArrayList<Integer> tmp1 = new ArrayList<Integer>();
  tmp1.add(1);
  return tmp1;
  }"))

(deftest propagated-types8-test
  (top-level-form
    '(defn f ^String [^Integer num]
       (let [i (atom num)]
         i))
    ;;->
    '(function f [num]
               (do (init i num)
                   (return i)))
    ;;->
    "public static final String f(final int num) {
int i = num;
return i;
}"))

(deftest propagated-types9-test
  (inner-form
    '(let [result (StringBuffer.)]
       result)
    ;;->
    '(do
       (init result (new StringBuffer))
       result)
    ;;->
    "final java.lang.StringBuffer result = new StringBuffer();"))

(deftest propagated-types10-test
  (inner-form
    '(let [^{:t {:mvector [:int]}} result (atom [])]
       @result)
    ;;->
    '(do
       (init result [])
       result)
    ;;->
    "final ArrayList<Integer> tmp1 = new ArrayList<Integer>();
ArrayList<Integer> result = tmp1;"))

(deftest foo2-test
  (top-level-form
    '(defn parse ^Integer [^String s]
       (nth s 1))
    ;;->
    '(function parse [s]
               (return (invoke clojure.lang.RT/nth s 1)))
    ;;->
    "public static final int parse(final String s) {
return s.charAt(1);
}"))
