(ns kalai.pass.java-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.pass.test-helpers :refer [ns-form top-level-form inner-form]]))

;; # Creating Variables

(deftest init1-test
  (top-level-form
    '(def ^{:t :int} x (int 3))
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
    '(let [^{:t :int} x (int 1)]
       x)
    ;;->
    '(do
       (init x 1)
       x)
    ;;->
    "final int x = 1;"))

(deftest init4-test
  (inner-form
    '(let [^Integer x (Integer. (int 1))]
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
      "static final io.lacuna.bifurcan.List<Object> x;
  static {
  final io.lacuna.bifurcan.List<Object> tmp1 = new io.lacuna.bifurcan.List<Object>();
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
      '(defn f ^Long [^Long x]
         (inc x))
      ;;->
      '(function f [x]
                 (return (operator + x 1)))
      ;;->
      "public static final long f(final long x) {
return (x + 1L);
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
         (+ x (int 1)))
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
import java.util.stream.Collectors;
public class TestClass {
public static final int f(final int x) {
return (x + 1);
}
public static final int f(final int x, final int y) {
return (x + y);
}
}
"))

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
       (reset! x (+ @x (int 2))))
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
       (println (+ @x (deref y))))
    ;;->
    '(do
       (init x 1)
       (init y 2)
       (init z 1)
       (do
         (assign x 3)
         (invoke println
                 (operator + x y))))
    ;;->
    "int x = 1;
int y = 2;
final int z = 1;
{
x = 3;
System.out.println((x + y));
}"))

(deftest local-variables3-test
  (inner-form
    '(with-local-vars [^int x (int 1)
                       ^int y (int 2)]
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
    "long y = (2L - 4L);
y = (y + 4L);"))

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
final long y = 5L;"))

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
import java.util.stream.Collectors;
public class TestClass {
static final HashMap<Long,String> x;
public static final HashMap<Long,String> f(final HashMap<Long,String> y) {
final HashMap<Long,String> z = y;
return z;
}
}
"))

;; If you are using heterogeneous data structures, the type you specify cannot be a nested type.
;; If you are casting, only cast heterogeneous.
;; The only time you don't know the type of the incoming value when casting is when dealing with heterogeneous :any.
;; So we will not provide a wide array of cast From implementations (as they are not useful).
;; In order to have (build up) nested heterogeneous data we need to have conversions to and from collection types whose element types are any.

;; copies much of sql_builder.core/cast-to-str
(deftest type-aliasing-and-casting-test
  (ns-form
    '((ns test-package.test-class)
      (def ^{:kalias {:mvector [:any]}} Clause) ;; Clause represents a part of a larger expression for a SQL keyword
      (defn f ^{:t :string} [^{:t :any} x]
        (let [v ^{:cast Clause} x
              ^{:t :any} v-first (nth v (int 0))
              ^{:t :string} table-name ^{:cast :string} v-first
              ^{:t :any} v-second (nth v (int 1))
              ^{:t :string} table-alias ^{:cast :string} v-second]
          (str table-name " AS " table-alias))))
    ;;->
    '(namespace test-package.test-class
                (function f [x]
                          (do
                            (init v x)
                            (init v-first (invoke clojure.lang.RT/nth v 0))
                            (init table-name v-first)
                            (init v-second (invoke clojure.lang.RT/nth v 1))
                            (init table-alias v-second)
                            (return
                              (invoke str table-name " AS " table-alias))
                            ;;(return nil)
                            )))
    ;;->
    "package testpackage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class TestClass {
public static final String f(final Object x) {
final ArrayList<Object> v = (ArrayList<Object>)x;
final Object vFirst = v.get(0);
final String tableName = (String)vFirst;
final Object vSecond = v.get(1);
final String tableAlias = (String)vSecond;
return (\"\" + tableName + \" AS \" + tableAlias);
}
}
"))

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
    "ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1L);
tmp1.add(2L);
final ArrayList<Long> x = tmp1;
System.out.println(x);"))

(deftest generic-types4-test
  (inner-form
    '(def ^{:t {:mmap [:string :string]}} x {:a "asdf"})
    ;;->
    '(init x {:a "asdf"})
    ;;->
    "HashMap<String,String> tmp1 = new HashMap<String,String>();
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

(deftest hyphen-test1
  (top-level-form
    '(def my-var 1)
    '(init my-var 1)
    "static final long myVar = 1L;"))

(deftest hyphen-test2
  (top-level-form
    '(defn my-function ^{:t :long} [^{:t :long} my-arg]
       (let [^{:t :long} my-binding 2]
         my-binding))
    '(function my-function [my-arg]
               (do
                 (init my-binding 2)
                 (return my-binding)))
    "public static final long myFunction(final long myArg) {
final long myBinding = 2L;
return myBinding;
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
System.out.println(1L);
}
else
{
System.out.println(2L);
}"))

;; # Data Literals

;; TODO: will change when we use a persistent collection library
(deftest data-literals-test
  (inner-form
    '(def ^{:t {:vector [:long]}} x [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "final io.lacuna.bifurcan.List<Long> x = new io.lacuna.bifurcan.List<Long>().addLast(1L).addLast(2L);"))

;; selecting between Vector and io.lacuna.bifurcan.List<Object>
(deftest data-literals2-test
  ;; TODO: inner form is not usually where def x would appear,
  ;; more likely as a top level form, but we haven't implemented static initializers yet
  (inner-form
    '(def x ^{:t {:mvector [:long]}} [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1L);
tmp1.add(2L);
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
    "ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1L);
tmp1.add(2L);
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
    "io.lacuna.bifurcan.List<Long> x = new io.lacuna.bifurcan.List<Long>().addLast(1L).addLast(2L);
x = new io.lacuna.bifurcan.List<Long>().addLast(3L).addLast(4L);"))

(deftest data-literals5-test
  (inner-form
    '(def x ^{:t {:map [:long :long]}} {1 2 3 4})
    ;;->
    '(init x {1 2 3 4})
    ;;->
    "final io.lacuna.bifurcan.Map<Long,Long> x = new io.lacuna.bifurcan.Map<Long,Long>().put(1L, 2L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put(3L, 4L, io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS);"))

(deftest data-literals6-test
  (inner-form
    '(def x ^{:t {:set [:long]}} #{1 2})
    ;;->
    '(init x #{1 2})
    ;;->
    "final io.lacuna.bifurcan.Set<Long> x = new io.lacuna.bifurcan.Set<Long>().add(1L).add(2L);"))

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
    "final io.lacuna.bifurcan.List<io.lacuna.bifurcan.List<Long>> x = new io.lacuna.bifurcan.List<io.lacuna.bifurcan.List<Long>>().addLast(new io.lacuna.bifurcan.List<Long>().addLast(1L)).addLast(new io.lacuna.bifurcan.List<Long>().addLast(2L));
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
      "final io.lacuna.bifurcan.List<io.lacuna.bifurcan.List<Long>> tmp1 = new io.lacuna.bifurcan.List<io.lacuna.bifurcan.List<Long>>();
  final io.lacuna.bifurcan.List<Long> tmp2 = new io.lacuna.bifurcan.List<Long>();
  tmp2.add(1);
  tmp1.add(tmp2);
  final io.lacuna.bifurcan.List<Long> tmp3 = new io.lacuna.bifurcan.List<Long>();
  tmp3.add(2);
  tmp1.add(tmp3);
  final io.lacuna.bifurcan.List<io.lacuna.bifurcan.List<Long>> x = tmp1;
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
    "HashMap<Long,ArrayList<String>> tmp1 = new HashMap<Long,ArrayList<String>>();
ArrayList<String> tmp2 = new ArrayList<String>();
tmp2.add(\"hi\");
tmp1.put(1L, tmp2);
ArrayList<String> tmp3 = new ArrayList<String>();
tmp3.add(\"hello\");
tmp3.add(\"there\");
tmp1.put(2L, tmp3);
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
    "final io.lacuna.bifurcan.List<io.lacuna.bifurcan.Map<io.lacuna.bifurcan.Set<Long>,io.lacuna.bifurcan.List<String>>> x = new io.lacuna.bifurcan.List<io.lacuna.bifurcan.Map<io.lacuna.bifurcan.Set<Long>,io.lacuna.bifurcan.List<String>>>().addLast(new io.lacuna.bifurcan.Map<io.lacuna.bifurcan.Set<Long>,io.lacuna.bifurcan.List<String>>().put(new io.lacuna.bifurcan.Set<Long>().add(1L), new io.lacuna.bifurcan.List<String>().addLast(\"hi\"), io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put(new io.lacuna.bifurcan.Set<Long>().add(2L), new io.lacuna.bifurcan.List<String>().addLast(\"hello\").addLast(\"there\"), io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS));
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
    "final io.lacuna.bifurcan.List<io.lacuna.bifurcan.Map<io.lacuna.bifurcan.Set<Long>,io.lacuna.bifurcan.List<String>>> x = new io.lacuna.bifurcan.List<io.lacuna.bifurcan.Map<io.lacuna.bifurcan.Set<Long>,io.lacuna.bifurcan.List<String>>>().addLast(new io.lacuna.bifurcan.Map<io.lacuna.bifurcan.Set<Long>,io.lacuna.bifurcan.List<String>>().put(new io.lacuna.bifurcan.Set<Long>().add(1L), new io.lacuna.bifurcan.List<String>().addLast(\"hi\"), io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS).put(new io.lacuna.bifurcan.Set<Long>().add(2L), new io.lacuna.bifurcan.List<String>().addLast(\"hello\").addLast(\"there\"), io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS));
System.out.println(x);"))

(deftest t
  (inner-form
    '(let [^{:t :long} x ^{:cast :long} (int 1)])
    '(do
       (init x 1)
       nil)
    "final long x = 1;"))

;; TODO: deprecated, we can't support any in all languages, so remove it
(deftest data-literals8-test
  (inner-form
    '(let [x ^{:t {:mvector [:any]}}
             [1
              ^{:t {:mvector [:long]}} [2]
              3
              ^{:t {:mvector [:any]}} [^{:t {:mvector [:long]}} [4]]]
           ^{:t :long} a ^{:cast :long} (nth x (int 0))]
       (println x)
       (println (+ 7 a)))
    ;;->
    '(do
       (init x [1 [2] 3 [[4]]])
       (init
         a
         (invoke
           clojure.lang.RT/nth
           x
           0))
       (do
         (invoke
           println
           x)
         (invoke
           println
           (operator
             +
             7
             a))))
    ;;->
    "ArrayList<Object> tmp1 = new ArrayList<Object>();
tmp1.add(1L);
ArrayList<Long> tmp2 = new ArrayList<Long>();
tmp2.add(2L);
tmp1.add(tmp2);
tmp1.add(3L);
ArrayList<Object> tmp3 = new ArrayList<Object>();
ArrayList<Long> tmp4 = new ArrayList<Long>();
tmp4.add(4L);
tmp3.add(tmp4);
tmp1.add(tmp3);
final ArrayList<Object> x = tmp1;
final long a = (long)x.get(0);
{
System.out.println(x);
System.out.println((7L + a));
}"))

;; TODO: deprecated, we can't support any in all languages, so remove it
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
    "HashMap<Object,Object> tmp1 = new HashMap<Object,Object>();
ArrayList<Object> tmp2 = new ArrayList<Object>();
HashMap<Long,Long> tmp3 = new HashMap<Long,Long>();
tmp3.put(2L, 3L);
tmp2.add(tmp3);
HashSet<Object> tmp4 = new HashSet<Object>();
tmp4.add(4L);
ArrayList<Long> tmp5 = new ArrayList<Long>();
tmp5.add(5L);
tmp5.add(6L);
tmp4.add(tmp5);
tmp2.add(tmp4);
tmp1.put(1L, tmp2);
final HashMap<Object,Object> x = tmp1;
System.out.println(x);"))

(deftest data-literals10-test
  (inner-form
    '(def ^{:t {:mmap [:string :long]}} x {"key" (+ 1 2)})
    ;;->
    '(init x {"key" (operator + 1 2)})
    ;;->
    "HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(\"key\", (1L + 2L));
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
    "ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1L);
tmp1.add(2L);
tmp1.add(3L);
tmp1.add(4L);
for (int x : tmp1) {
System.out.println(x);
}"))

(deftest for-loop-test
  (inner-form
    '(dotimes [^{:t :int} x (int 5)]
       (println x))
    ;;->
    '(group
       (init x 0)
       (while (operator < x 5)
         (invoke println x)
         (assign x (operator + x 1))))
    ;;->
    "int x = 0;
while ((x < 5)) {
System.out.println(x);
x = (x + 1);
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
System.out.println(1L);
}
else
{
if (false)
{
System.out.println(2L);
}
else
{
if (true)
{
System.out.println(3L);
}
}
}"))

(deftest keywords-as-functions-test
  (inner-form
    '(:k ^{:t {:mmap [:string :long]}} {:k 1})
    ;;->
    '(invoke clojure.lang.RT/get {:k 1} :k)
    ;;->
    "HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(\":k\", 1L);
tmp1.get(\":k\");"))

(deftest keywords-as-functions2-test
  (inner-form
    '(:k ^{:t {:mset [:string]}} #{:k})
    ;;->
    '(invoke clojure.lang.RT/get #{:k} :k)
    ;;->
    "HashSet<String> tmp1 = new HashSet<String>();
tmp1.add(\":k\");
tmp1.get(\":k\");"))

(deftest collection-put-get-test
  (inner-form
    '(let [k "k"
           m ^{:t {:mmap [:string :long]}} {k 1}
           ^{:t :long} v (get m k)]
       v)
    ;;->
    '(do
       (init k "k")
       (init m {k 1})
       (init v (invoke clojure.lang.RT/get m k))
       v)
    ;;->
    "final String k = \"k\";
HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(k, 1L);
final HashMap<String,Long> m = tmp1;
final long v = m.get(k);"
    ))

#_(deftest collection-closure-test
  (inner-form
    '(let [c [1 2 3]
           f (fn [] (conj c 4))
           g (fn [] (conj c 5))
           d (f)
           e (g)]
       c)
    ;;->
    '()
    ;;->
    ""
    ))

;; TODO: due to a quirk of Clojure, cases can be ints, I don't think this will compile
(deftest switch-case-test
  (inner-form
    '(case 1
       1 :a
       2 :b
       :c)
    ;;->
    '(case 1 {1 [1 :a]
              2 [2 :b]}
             :c)
    ;;->
    "switch (1L) {
case 1 : \":a\";
break;
case 2 : \":b\";
break;
default : \":c\"
break;
}"))


;; TODO: support switch as expression
(deftest switch-case2-test
  #_(inner-form
    '(println (case 1
                1 :a
                2 :b))
    ;;->
    '(invoke println
             (case 1 {1 [1 :a]
                      2 [2 :b]}))
    ;;->
    "switch (1) {
case 1 : \":a\";
break;
case 2 : \":b\";
break;
}"))

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
tmp1 = 1L;
}
else
{
tmp1 = 2L;
}
long tmp2;
if (true)
{
long tmp3;
if (true)
{
tmp3 = 3L;
}
else
{
tmp3 = 4L;
}
{
tmp2 = tmp3;
}
}
else
{
tmp2 = 5L;
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
                2)
            3)
          4))
    ;;->
    '(invoke println
             (operator +
                       (if true
                         (do
                           (invoke println 1)
                           2)
                         3)
                       4))
    ;;->
    "long tmp1;
if (true)
{
System.out.println(1L);
{
tmp1 = 2L;
}
}
else
{
tmp1 = 3L;
}
System.out.println((tmp1 + 4L));"))


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
tmp1 = 1L;
}
else
{
long tmp2;
if (false)
{
tmp2 = 2L;
}
else
{
tmp2 = 3L;
}
{
tmp1 = tmp2;
}
}
System.out.println((tmp1 + 4L));"))

(deftest operator-test
  (inner-form
    '(println
       (not (= 1 (inc 1))))
    ;;->
    '(invoke println
             (operator ! (operator == 1 (operator + 1 1))))
    ;;->
    "System.out.println(!(1L == (1L + 1L)));"))

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

(deftest interop2-test
  (inner-form
    '(assoc ^{:t {:mmap [:string :long]}} {:a 1} :b 2)
    ;;->
    '(invoke assoc {:a 1} :b 2)
    ;;->
    "HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(\":a\", 1L);
tmp1.put(\":b\", 2L);"))

(deftest interop3-test
  (inner-form
    '(update ^{:t {:mmap [:string :long]}} {:a 1} :a inc)
    ;;->
    '(invoke update {:a 1} :a inc)
    ;;->
    "HashMap<String,Long> tmp1 = new HashMap<String,Long>();
tmp1.put(\":a\", 1L);
tmp1.put(\":a\", (tmp1.get(\":a\") + 1L));"))

(deftest interop4-test
  (inner-form
    '(def ^{:t :int} x (count "abc"))
    ;;->
    '(init x (invoke clojure.lang.RT/count "abc"))
    ;;->
    "final int x = \"abc\".length();"))

(deftest interop5-test
  (inner-form
    '(let [^String s "abc"]
       (println (nth s (int 1))))
    ;;->
    '(do
       (init s "abc")
       (invoke println
               (invoke clojure.lang.RT/nth s 1)))
    ;;->
    "final String s = \"abc\";
System.out.println(s.charAt(1));"))

(deftest interop6-test
  (inner-form
    '(let [^{:t {:mvector [:int]}} v [(int 1) (int 2) (int 3)]]
       (println (nth v (int 1))))
    ;;->
    '(do
       (init v [1 2 3])
       (invoke println
               (invoke clojure.lang.RT/nth v 1)))
    ;;->
    "ArrayList<Integer> tmp1 = new ArrayList<Integer>();
tmp1.add(1);
tmp1.add(2);
tmp1.add(3);
final ArrayList<Integer> v = tmp1;
System.out.println(v.get(1));"))

(deftest interop7-test
  (inner-form
    '(let [result (atom ^{:t {:mvector [:int]}} [])
           i (atom (int 10))]
       (while (< (int 0) @i)
         (swap! result conj @i)
         (reset! i (- @i (int 3)))))
    ;;->
    '(do
       (init result [])
       (init i 10)
       (while (operator < 0 i)
         (invoke conj result i)
         (assign i (operator - i 3))))
    ;;->
    "ArrayList<Integer> result = new ArrayList<Integer>();
int i = 10;
while ((0 < i)) {
result.add(i);
i = (i - 3);
}"))

(deftest interop8-test
  (inner-form
    '(let [^{:t {:mvector [:int]}} separatorPositions []
           ^{:t :int} numPositions (.size separatorPositions)]
       (println "hi"))
    ;;->
    '(do
       (init separatorPositions [])
       (init numPositions
             (method size separatorPositions))
       (invoke println "hi"))
    ;;->
    "final ArrayList<Integer> separatorPositions = new ArrayList<Integer>();
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
return 1L;
}
else
{
final long x = 2L;
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
    "final long x = 1L;
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
    "final int x = 1L;
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
    "final long x = 1L;
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
    "final ArrayList<Integer> x = new ArrayList<Integer>();
final ArrayList<Integer> y = x;
final ArrayList<Integer> z = y;
System.out.println(z);"))

(deftest propagated-types3-test
  (inner-form
    '(let [a (atom 1)
           ;; TODO: shouldn't have to declare the type of x here
           ^{:t :long} x (cond
                           true @a
                           false @a
                           :else @a)]
       (println x))
    ;;->
    '(do
       (init a 1)
       (init x (if true
                 a
                 (if false
                   a
                   a)))
       (invoke println x))
    ;;->
    "long a = 1L;
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
else
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
    "ArrayList<Long> x = new ArrayList<Long>();
ArrayList<Long> tmp1 = new ArrayList<Long>();
tmp1.add(1L);
tmp1.add(2L);
tmp1.add(3L);
x = tmp1;"))

(deftest propagated-types5-test
  (inner-form
    ;; TODO: it would be nice to annotate x here instead
    '(let [x (atom ^{:t {:mvector [:long]}} [])]
       (swap! x conj 1))
    ;;->
    '(do
       (init x [])
       (invoke conj x 1))
    ;;->
    "ArrayList<Long> x = new ArrayList<Long>();
x.add(1L);"))

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
    '(defn f ^Integer [^Integer num]
       (let [i (atom num)]
         i))
    ;;->
    '(function f [num]
               (do (init i num)
                   (return i)))
    ;;->
    "public static final int f(final int num) {
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
    "ArrayList<Integer> result = new ArrayList<Integer>();"))

(deftest annotate-type-const-test
  (ns-form
    '((ns test-package.test-class)
      (def ^{:kalias {:mmap [:long :string]}} T)
      (def ^{:t T} x)
      (defn f ^{:t T} [^{:t T} y]
        ^{:t T} {1 "hahaha"}))
    ;;->
    '(namespace test-package.test-class
                (init x)
                (function f [y]
                          (return
                            {1 "hahaha"})))
    ;;->
    "package testpackage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
public class TestClass {
static final HashMap<Long,String> x;
public static final HashMap<Long,String> f(final HashMap<Long,String> y) {
HashMap<Long,String> tmp1 = new HashMap<Long,String>();
tmp1.put(1L, \"hahaha\");
return tmp1;
}
}
"))

;; TODO:
(deftest destructure-test
  #_(inner-form
    '(let [{:keys [a b]} {:a "a" :b "b"}]
       a)
    ;;->
    '()
    ;;->
    ""))
