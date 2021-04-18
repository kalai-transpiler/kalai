(ns kalai.pass.rust-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.pass.test-helpers
             :refer [ns-form-rust top-level-form-rust inner-form-rust]
             :rename {ns-form-rust ns-form
                      top-level-form-rust top-level-form
                      inner-form-rust inner-form}]))

;; # Creating Variables

;; TODO: this should be a static (top level "def")
(deftest init1-test
  (top-level-form
    '(def ^{:t :int} x 3)
    ;;->
    '(init x 3)
    ;;->
    "lazy_static! {
static ref x: i32 = 3;
}"))

(deftest init2-test
  (top-level-form
    '(def ^Integer x)
    ;;->
    '(init x)
    ;;->
    "lazy_static! {
static ref x: i32 = ();
}"))

(deftest init3-test
  (inner-form
    '(let [^{:t :int} x 1]
       x)
    ;;->
    '(do
       (init x 1)
       x)
    ;;->
    "let x: i32 = 1;
x;"))

;; TODO: do we even need to support this for Rust?
#_(deftest init4-test
  (inner-form
    '(let [^Integer x (Integer. 1)]
       x)
    ;;->
    '(do
       (init x (new Integer 1))
       x)
    ;;->
    "let x: i32 = new Integer(1);"))


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
      "pub fn f(x: i32) -> i32 {
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

;; TODO: make this work for Rust. maybe append arity number to
;; end of transpiled Rust fn name to disambiguate
;; Multiple arity aka overloaded methods
#_(deftest function3-test
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
       (println x x))
    ;;->
    '(function f [x]
               (invoke println x x))
    ;;->
    "pub fn f(x: i64) -> () {
println!(\"{} {}\", x, x);
}"))

(deftest local-variables-test
  (inner-form
    '(let [x (atom (int 0))]
       (reset! x (+ @x 2)))
    ;;->
    '(do
       (init x 0)
       (assign x (operator + x 2)))
    ;;->
    "let mut x: i32 = 0;
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
         (invoke println (operator + x y))))
    ;;->
    "let mut x: i32 = 1;
let mut y: i32 = 2;
let z: i32 = 1;
{
x = 3;
println!(\"{}\", (x + y));
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
    "let mut x: i32 = 1;
let mut y: i32 = 2;
println!(\"{}\", (x + y));"))

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
      ""))

'(defn f {:t :void} []
   (let [^:mut ^{:t :long} y (atom (- 2 4))]
     (swap! y + 4)))

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
    "let mut y: i64 = (2 - 4);
y = (y + 4);
y;"))

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
    "let x: bool = true;
let x: bool = true;
{
let z: bool = true;
z;
}
let y: i64 = 5;"))

(deftest type-aliasing-test
  (ns-form
    '((ns test-package.test-class)
      (def ^{:kalias {:mmap [:long :string]}} T)
      (def ^{:t T} x {})
      (defn f ^{:t T} [^{:t T} y]
        (let [^{:t T} z y]
          z)))
    ;;->
    '(namespace test-package.test-class
                (init x {})
                (function f [y]
                          (do
                            (init z y)
                            (return z))))
    ;;->
    "#[macro_use]
extern crate lazy_static;
use std::collections::HashMap;
use std::collections::HashSet;
use std::vec::Vec;
use std::env;
lazy_static::lazy_static! {
static ref x: HashMap<i64,String> = {
let mut tmp_1: HashMap<i64,String> = HashMap::new();
tmp_1
};
}
pub fn f(y: HashMap<i64,String>) -> HashMap<i64,String> {
let z: HashMap<i64,String> = y;
return z;
}"))

;; TODO: figure out nil strategy for Rust
(deftest generic-types-test
  #_(top-level-form
    '(def ^{:t {:mmap [:long :string]}} x)
    ;;->
    '(init x)
    ;;->
    "lazy_static! {
static ref x: HashMap<i64,String> = ();
}"))

(deftest generic-types2-test
  #_(top-level-form
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
    "let x: Vec<i64> = {
let mut tmp_1: Vec<i64> = Vec::new();
tmp_1.push(1);
tmp_1.push(2);
tmp_1
};
println!(\"{}\", x);"))

(deftest generic-types4-test
  (inner-form
    '(def ^{:t {:mmap [:string :string]}} x {:a "asdf"})
    ;;->
    '(init x {:a "asdf"})
    ;;->
    "let x: HashMap<String,String> = {
let mut tmp_1: HashMap<String,String> = HashMap::new();
tmp_1.insert(String::from(\":a\"), String::from(\"asdf\"));
tmp_1
};"))

(deftest generic-types5-test
  #_(inner-form
      '(let [x ^{:t {:array [:string]}} ["arg1" "arg2"]]
         (println x))
      ;;->
      '(do
         (init x ["arg1" "arg2"])
         (invoke println x))
      ;;->
      ""))

;; # Main entry point

(deftest main-method-test
  (top-level-form
    ;; return type of `void` for `main()` is implied
    '(defn -main ^{:t :void} [& my-args]
       (println 1))
    ;;->
    '(function -main [& my-args] (invoke println 1))
    ;;->
    "fn main () {
let my_args: Vec<String> = env::args().collect();
{
println!(\"{}\", 1);
}
}"))

(deftest hyphen-test1
  (top-level-form
    '(def my-var 1)
    '(init my-var 1)
    "lazy_static! {
static ref my_var: i64 = 1;
}"))

(deftest hyphen-test2
  (top-level-form
    '(defn my-function ^{:t :long} [^{:t :long} my-arg]
       (let [^{:t :long} my-binding 2]
         my-binding))
    '(function my-function [my-arg]
               (do
                 (init my-binding 2)
                 (return my-binding)))
    "pub fn my_function(my_arg: i64) -> i64 {
let my_binding: i64 = 2;
return my_binding;
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
    "if true
{
println!(\"{}\", 1);
}
else
{
println!(\"{}\", 2);
}"))

;; # Data Literals

;; TODO: will change when we use a persistent collection library
(deftest data-literals-test
  (inner-form
    '(def ^{:t {:vector [:long]}} x [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "let x: PVector<i64> = {
let mut tmp_1: PVector<i64> = PVector::new();
tmp_1.push(1);
tmp_1.push(2);
tmp_1
};"))

(deftest data-literals2-test
  (top-level-form
    '(def x ^{:t {:mvector [:long]}} [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "lazy_static! {
static ref x: Vec<i64> = {
let mut tmp_1: Vec<i64> = Vec::new();
tmp_1.push(1);
tmp_1.push(2);
tmp_1
};
}"))

(deftest data-literals3-test
  (inner-form
    '(let [^{:t {:mvector [:long]}} x [1 2]]
       x)
    ;;->
    '(do
       (init x [1 2])
       x)
    ;;->
    "let x: Vec<i64> = {
let mut tmp_1: Vec<i64> = Vec::new();
tmp_1.push(1);
tmp_1.push(2);
tmp_1
};
x;"))

(deftest data-literals4-test
  (inner-form
    '(let [x (atom ^{:t {:vector [:long]}} [1 2])]
       (reset! x ^{:t {:vector [:long]}} [3 4]))
    ;;->
    '(do
       (init x [1 2])
       (assign x [3 4]))
    ;;->
    "let mut x: PVector<i64> = {
let mut tmp_1: PVector<i64> = PVector::new();
tmp_1.push(1);
tmp_1.push(2);
tmp_1
};
x = {
let mut tmp_2: PVector<i64> = PVector::new();
tmp_2.push(3);
tmp_2.push(4);
tmp_2
};"))

(deftest data-literals5-test
  (inner-form
    '(def x ^{:t {:map [:long :long]}} {1 2 3 4})
    ;;->
    '(init x {1 2 3 4})
    ;;->
    "let x: PMap<i64,i64> = {
let mut tmp_1: PMap<i64,i64> = PMap::new();
tmp_1.insert(1, 2);
tmp_1.insert(3, 4);
tmp_1
};"))

(deftest data-literals6-test
  (inner-form
    '(def x ^{:t {:set [:long]}} #{1 2})
    ;;->
    '(init x #{1 2})
    ;;->
    "let x: PSet<i64> = {
let mut tmp_1: PSet<i64> = PSet::new();
tmp_1.insert(1);
tmp_1.insert(2);
tmp_1
};"))

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
    "let x: PVector<PVector<i64>> = {
let mut tmp_1: PVector<PVector<i64>> = PVector::new();
tmp_1.push({
let mut tmp_2: PVector<i64> = PVector::new();
tmp_2.push(1);
tmp_2
});
tmp_1.push({
let mut tmp_3: PVector<i64> = PVector::new();
tmp_3.push(2);
tmp_3
});
tmp_1
};
println!(\"{}\", x);"))

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
      ""))

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
    "let x: HashMap<i64,Vec<String>> = {
let mut tmp_1: HashMap<i64,Vec<String>> = HashMap::new();
tmp_1.insert(1, {
let mut tmp_2: Vec<String> = Vec::new();
tmp_2.push(String::from(\"hi\"));
tmp_2
});
tmp_1.insert(2, {
let mut tmp_3: Vec<String> = Vec::new();
tmp_3.push(String::from(\"hello\"));
tmp_3.push(String::from(\"there\"));
tmp_3
});
tmp_1
};
println!(\"{}\", x);"))

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
    "let x: PVector<PMap<PSet<i64>,PVector<String>>> = {
let mut tmp_1: PVector<PMap<PSet<i64>,PVector<String>>> = PVector::new();
tmp_1.push({
let mut tmp_2: PMap<PSet<i64>,PVector<String>> = PMap::new();
tmp_2.insert({
let mut tmp_3: PSet<i64> = PSet::new();
tmp_3.insert(1);
tmp_3
}, {
let mut tmp_4: PVector<String> = PVector::new();
tmp_4.push(String::from(\"hi\"));
tmp_4
});
tmp_2.insert({
let mut tmp_5: PSet<i64> = PSet::new();
tmp_5.insert(2);
tmp_5
}, {
let mut tmp_6: PVector<String> = PVector::new();
tmp_6.push(String::from(\"hello\"));
tmp_6.push(String::from(\"there\"));
tmp_6
});
tmp_2
});
tmp_1
};
println!(\"{}\", x);"))

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
    "let x: PVector<PMap<PSet<i64>,PVector<String>>> = {
let mut tmp_1: PVector<PMap<PSet<i64>,PVector<String>>> = PVector::new();
tmp_1.push({
let mut tmp_2: PMap<PSet<i64>,PVector<String>> = PMap::new();
tmp_2.insert({
let mut tmp_3: PSet<i64> = PSet::new();
tmp_3.insert(1);
tmp_3
}, {
let mut tmp_4: PVector<String> = PVector::new();
tmp_4.push(String::from(\"hi\"));
tmp_4
});
tmp_2.insert({
let mut tmp_5: PSet<i64> = PSet::new();
tmp_5.insert(2);
tmp_5
}, {
let mut tmp_6: PVector<String> = PVector::new();
tmp_6.push(String::from(\"hello\"));
tmp_6.push(String::from(\"there\"));
tmp_6
});
tmp_2
});
tmp_1
};
println!(\"{}\", x);"))

(deftest data-literals8-test
  ;; Rust has an Any trait, but no Any type...
  ;; therefore Kalai can't support Any.
  ;; We think we can use an enum like Serde does
  #_(inner-form
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
    ""))

;; omitted 9 because of any....

(deftest data-literals10-test
  (inner-form
    '(def ^{:t {:mmap [:string :long]}} x {"key" (+ 1 2)})
    ;;->
    '(init x {"key" (operator + 1 2)})
    ;;->
    "let x: HashMap<String,i64> = {
let mut tmp_1: HashMap<String,i64> = HashMap::new();
tmp_1.insert(String::from(\"key\"), (1 + 2));
tmp_1
};"))

(deftest string-equality-test
  (inner-form
    '(println (= "abc" "abc"))
    ;;->
    '(invoke println (operator == "abc" "abc"))
    ;;->
    "println!(\"{}\", (String::from(\"abc\") == String::from(\"abc\")));"))

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
    "let x: String = String::from(\"abc\");
let y: String = String::from(\"abc\");
println!(\"{}\", (x == y));"))

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
    "let x: String = String::new(String::from(\"abc\"));
let y: String = String::from(\"abc\");
println!(\"{}\", (x == y));"))

(deftest foreach-test
  (inner-form
    '(doseq [^int x ^{:t {:mvector [:long]}} [1 2 3 4]]
       (println x))
    ;;->
    '(foreach x [1 2 3 4]
              (invoke println x))
    ;;->
    "for x in {
let mut tmp_1: Vec<i64> = Vec::new();
tmp_1.push(1);
tmp_1.push(2);
tmp_1.push(3);
tmp_1.push(4);
tmp_1
} {
println!(\"{}\", x);
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
         (assign x (operator + x 1))))
    ;;->
    "let mut x: i32 = 0;
while (x < 5) {
println!(\"{}\", x);
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
    "while true {
println!(\"{}\", String::from(\"hi\"));
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
    "if true
{
println!(\"{}\", 1);
}
else
{
if false
{
println!(\"{}\", 2);
}
else
{
if true
{
println!(\"{}\", 3);
}
}
}"))

(deftest keywords-as-functions-test
  (inner-form
    '(println (:k ^{:t {:mmap [:string :long]}} {:k 1}))
    ;;->
    '(invoke println (invoke clojure.lang.RT/get {:k 1} :k))
    ;;->
    "println!(\"{}\", {
let mut tmp_1: HashMap<String,i64> = HashMap::new();
tmp_1.insert(String::from(\":k\"), 1);
tmp_1
}.get(&String::from(\":k\")).unwrap());"))

(deftest keywords-as-functions2-test
  (inner-form
    '(:k ^{:t {:mset [:string]}} #{:k})
    ;;->
    '(invoke clojure.lang.RT/get #{:k} :k)
    ;;->
    "{
let mut tmp_1: HashSet<String> = HashSet::new();
tmp_1.insert(String::from(\":k\"));
tmp_1
}.get(&String::from(\":k\")).unwrap();"))

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
    "match 1 {
1 => String::from(\":a\"),
2 => String::from(\":b\"),
_ => String::from(\":c\"),
};"))

(deftest switch-case2-test
  (inner-form
      '(println (case 1
                  1 :a
                  2 :b
                  :c))
      ;;->
      '(invoke println
               (case 1 {1 [1 :a]
                        2 [2 :b]}
                       :c))
      ;;->
      "println!(\"{}\", match 1 {
1 => String::from(\":a\"),
2 => String::from(\":b\"),
_ => String::from(\":c\"),
});"))

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
    "println!(\"{}\", (if true
{
1
}
else
{
2
} + if true
{
if true
{
3
}
else
{
4
}
}
else
{
5
}));"))

;; Note: Rust will not compile when conditionals as expressions don't have
;; an "else" branch (that is, only has a "then" branch)
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
    "println!(\"{}\", (if true
{
println!(\"{}\", 1);
2
}
else
{
3
} + 4));"))

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
    "println!(\"{}\", (if true
{
1
}
else
{
if false
{
2
}
else
{
3
}
} + 4));"))

(deftest operator-test
  (inner-form
    '(println
       (not (= 1 (inc 1))))
    ;;->
    '(invoke println
             (operator ! (operator == 1 (operator + 1 1))))
    ;;->
    "println!(\"{}\", !(1 == (1 + 1)));"))


(deftest interop-test
  (inner-form
    '(let [^{:t :string} a (new String)
           b (String.)
           c (new String)]
       (.length a)
       (. b length)
       (.length c))
    ;;->
    '(do
       (init a (new String))
       (init b (new String))
       (init c (new String))
       (do
         (method length a)
         (method length b)
         (method length c)))
    ;;->
    "let a: String = String::new();
let b: String = String::new();
let c: String = String::new();
{
a.chars().count() as i32;
b.chars().count() as i32;
c.chars().count() as i32;
}"))

(deftest interop1b-test
  (inner-form
    '(let [^StringBuffer a (new StringBuffer)
           b (StringBuffer.)]
       (.length a)
       (. b length))
    ;;->
    '(do
       (init a (new StringBuffer))
       (init b (new StringBuffer))
       (do
         (method length a)
         (method length b)))
    ;;->
    "let a: Vec<char> = Vec::new();
let b: Vec<char> = Vec::new();
{
a.len() as i32;
b.len() as i32;
}"))

(deftest interop2-test
  (inner-form
    '(assoc ^{:t {:mmap [:string :long]}} {:a 1} :b 2)
    ;;->
    '(invoke assoc {:a 1} :b 2)
    ;;->
    "{
let mut tmp_1: HashMap<String,i64> = HashMap::new();
tmp_1.insert(String::from(\":a\"), 1);
tmp_1
}.insert(String::from(\":b\"), 2);"))

(deftest interop3-test
  (inner-form
    '(update ^{:t {:mmap [:string :long]}} {:a 1} :a inc)
    ;;->
    '(invoke update {:a 1} :a inc)
    ;;->
    "{
let mut tmp_1: HashMap<String,i64> = HashMap::new();
tmp_1.insert(String::from(\":a\"), 1);
tmp_1
}.insert(String::from(\":a\"), ({
let mut tmp_1: HashMap<String,i64> = HashMap::new();
tmp_1.insert(String::from(\":a\"), 1);
tmp_1
}.get(&String::from(\":a\")).unwrap() + 1));"))

(deftest interop4-test
  (inner-form
    '(def ^{:t :int} x (count "abc"))
    ;;->
    '(init x (invoke clojure.lang.RT/count "abc"))
    ;;->
    "let x: i32 = String::from(\"abc\").len() as i32;"))

;; Because Rust strings semantically differ from Java strings, we're not even
;; sure if `nth` on strings even makes sense across languages. If/when we
;; revisit, we could instead offer an iterator over a string as a construct
;; in input Kalai.
(deftest interop5-test
  #_(inner-form
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

(deftest interop6-test
  (inner-form
    '(let [^{:t {:mvector [:int]}} v [1 2 3]]
       (println (nth v 1)))
    ;;->
    '(do
       (init v [1 2 3])
       (invoke println
               (invoke clojure.lang.RT/nth v 1)))
    ;;->
    "let v: Vec<i32> = {
let mut tmp_1: Vec<i32> = Vec::new();
tmp_1.push(1);
tmp_1.push(2);
tmp_1.push(3);
tmp_1
};
println!(\"{}\", *v.get(1 as usize).unwrap());"))


(deftest interop7-test
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
         (invoke conj result i)
         (assign i (operator - i 3))))
    ;;->
    "let mut result: Vec<i32> = {
let mut tmp_1: Vec<i32> = Vec::new();
tmp_1
};
let mut i: i32 = 10;
while (0 < i) {
result.push(i);
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
    "let separator_positions: Vec<i32> = {
let mut tmp_1: Vec<i32> = Vec::new();
tmp_1
};
let num_positions: i32 = separator_positions.len() as i32;
println!(\"{}\", String::from(\"hi\"));"))


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
    "pub fn f() -> i64 {
if true
{
return 1;
}
else
{
let x: i64 = 2;
{
println!(\"{}\", String::from(\"hi\"));
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
    "let x: i64 = 1;
let y: i64 = x;
println!(\"{}\", y);"))


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
    "let x: i32 = 1;
let y: i32 = x;
println!(\"{}\", y);"))


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
    "let mut a: i64 = 1;
let x: i64 = if true
{
a
}
else
{
if false
{
a
}
else
{
a
}
};
println!(\"{}\", x);"))


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
    "let mut x: Vec<i64> = {
let mut tmp_1: Vec<i64> = Vec::new();
tmp_1
};
x = {
let mut tmp_2: Vec<i64> = Vec::new();
tmp_2.push(1);
tmp_2.push(2);
tmp_2.push(3);
tmp_2
};"))

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
    "let mut x: Vec<i64> = {
let mut tmp_1: Vec<i64> = Vec::new();
tmp_1
};
x.push(1);"))


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
    "pub fn f(s: String) -> () {
let x: String = s;
println!(\"{}\", x);
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
    "pub fn f(num: i32) -> i32 {
let mut i: i32 = num;
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
    "let result: Vec<char> = Vec::new();
result;"))

(deftest propagated-types10-test
  (inner-form
    '(let [^{:t {:mvector [:int]}} result (atom [])]
       @result)
    ;;->
    '(do
       (init result [])
       result)
    ;;->
    "let mut result: Vec<i32> = {
let mut tmp_1: Vec<i32> = Vec::new();
tmp_1
};
result;"))
