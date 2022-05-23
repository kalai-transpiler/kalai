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
    '(def ^{:t :int} x (int 3))
    ;;->
    '(init x 3)
    ;;->
    "lazy_static::lazy_static! {
static ref x: i32 = 3i32;
}"))

;; TODO: this test doesn't make sense in Rust because top-level bindings can't be empty
(deftest init2-test
  (top-level-form
    '(def ^{:t :any} x)
    ;;->
    '(init x)
    ;;->
    "lazy_static::lazy_static! {
static ref x: kalai::BValue = kalai::BValue::from(kalai::NIL);
}"))

(deftest init3-test
  (inner-form
    '(let [^{:t :int} x (int 1)]
       x)
    ;;->
    '(do
       (init x 1)
       x)
    ;;->
    "let x: i32 = 1i32;
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
      "static final rpds::Vector<Object> x;
  static {
  final rpds::Vector<Object> tmp1 = new rpds::Vector<Object>();
  tmp1.add(1);
  tmp1.add(2);
  tmp1.add(3);
  x = tmp1;
  }"))

;; # Declare
;; (declarations are erased in kalai pipeline's kalai-constructs)

(deftest declare-test
  (top-level-form
    '(declare ^{:t :int} x)
    ;;->
    nil
    ;;->
    ""))

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
return (x + 1i32);
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
       (reset! x (+ @x (int 2))))
    ;;->
    '(do
       (init x 0)
       (assign x (operator + x 2)))
    ;;->
    "let mut x: i32 = 0i32;
x = (x + 2i32);"))

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
    "let mut x: i32 = 1i32;
let mut y: i32 = 2i32;
let z: i32 = 1i32;
{
x = 3i32;
println!(\"{}\", (x + y));
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
    "let mut x: i32 = 1i32;
let mut y: i32 = 2i32;
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
    "let mut y: i64 = (2i64 - 4i64);
y = (y + 4i64);
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
let y: i64 = 5i64;"))

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
    "use crate::kalai;
use crate::kalai::PMap;
lazy_static::lazy_static! {
static ref x: std::collections::HashMap<i64,String> = std::collections::HashMap::new();
}
pub fn f(y: std::collections::HashMap<i64,String>) -> std::collections::HashMap<i64,String> {
let z: std::collections::HashMap<i64,String> = y;
return z;
}"))

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
    "use crate::kalai;
use crate::kalai::PMap;
pub fn f(x: kalai::BValue) -> String {
let v: std::vec::Vec<kalai::BValue> = std::vec::Vec::from(x);
let v_first: kalai::BValue = v.get(0i32 as usize).unwrap().clone();
let table_name: String = String::from(v_first);
let v_second: kalai::BValue = v.get(1i32 as usize).unwrap().clone();
let table_alias: String = String::from(v_second);
return format!(\"{}{}{}\", table_name, String::from(\" AS \"), table_alias);
}"))

;; copies much of test type-aliasing-and-casting-test
(deftest type-aliasing-and-conj-test
  (ns-form
    '((ns test-package.test-class)
      (def ^{:kalias {:mvector [:int]}} SeparatorPositions)
      (defn getSeparatorPositions ^{:t :void}
        []
        (let [^{:t SeparatorPositions} result (atom [])]
          (swap! result conj 3)))
      )
    ;;->
    '(namespace
       test-package.test-class
       (function getSeparatorPositions []
         (do
           (init result [])
           (invoke conj result 3))))
    ;;->
    "use crate::kalai;
use crate::kalai::PMap;
pub fn get_separator_positions() -> () {
let mut result: std::vec::Vec<i32> = std::vec::Vec::new();
result.push(3i64);
}"))

;; TODO: figure out nil strategy for Rust
(deftest generic-types-test
  #_(top-level-form
    '(def ^{:t {:mmap [:long :string]}} x)
    ;;->
    '(init x)
    ;;->
    "lazy_static::lazy_static! {
static ref x: std::collections::HashMap<i64,String> = ();
}"))

(deftest generic-types2-test
  #_(top-level-form
    '(def ^{:t {:mmap [:string {:mvector [:char]}]}} x)
    ;;->
    '(init x)
    ;;->
    "static final std::collections::HashMap<String,ArrayList<Character>> x;"))


(deftest generic-types3-test
  (inner-form
    '(let [x ^{:t {:mvector [:long]}} [1 2]]
       (println x))
    ;;->
    '(do
       (init x [1 2])
       (invoke println x))
    ;;->
    "let x: std::vec::Vec<i64> = {
let mut tmp1: std::vec::Vec<i64> = std::vec::Vec::new();
tmp1.push(1i64);
tmp1.push(2i64);
tmp1
};
println!(\"{}\", x);"))

(deftest generic-types4-test
  (inner-form
    '(def ^{:t {:mmap [:string :string]}} x {:a "asdf"})
    ;;->
    '(init x {:a "asdf"})
    ;;->
    "let x: std::collections::HashMap<String,String> = {
let mut tmp1: std::collections::HashMap<String,String> = std::collections::HashMap::new();
tmp1.insert(String::from(\":a\"), String::from(\"asdf\"));
tmp1
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
    "pub fn main () {
let my_args: std::vec::Vec<String> = std::env::args().collect();
{
println!(\"{}\", 1i64);
}
}"))

(deftest hyphen-test1
  (top-level-form
    '(def my-var 1)
    '(init my-var 1)
    "lazy_static::lazy_static! {
static ref my_var: i64 = 1i64;
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
let my_binding: i64 = 2i64;
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
println!(\"{}\", 1i64);
}
else
{
println!(\"{}\", 2i64);
}"))

;; # Data Literals

;; TODO: will change when we use a persistent collection library
(deftest data-literals-test
  (inner-form
    '(def ^{:t {:vector [:long]}} x [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "let x: rpds::Vector<i64> = rpds::Vector::new().push_back(1i64).push_back(2i64);"))

(deftest data-literals2-test
  (top-level-form
    '(def x ^{:t {:mvector [:long]}} [1 2])
    ;;->
    '(init x [1 2])
    ;;->
    "lazy_static::lazy_static! {
static ref x: std::vec::Vec<i64> = {
let mut tmp1: std::vec::Vec<i64> = std::vec::Vec::new();
tmp1.push(1i64);
tmp1.push(2i64);
tmp1
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
    "let x: std::vec::Vec<i64> = {
let mut tmp1: std::vec::Vec<i64> = std::vec::Vec::new();
tmp1.push(1i64);
tmp1.push(2i64);
tmp1
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
    "let mut x: rpds::Vector<i64> = rpds::Vector::new().push_back(1i64).push_back(2i64);
x = rpds::Vector::new().push_back(3i64).push_back(4i64);"))

(deftest data-literals5-test
  (inner-form
    '(def x ^{:t {:map [:long :long]}} {1 2 3 4})
    ;;->
    '(init x {1 2 3 4})
    ;;->
    "let x: rpds::HashTrieMap<i64,i64> = rpds::HashTrieMap::new().insert(1i64, 2i64).insert(3i64, 4i64);"))

(deftest data-literals6-test
  (inner-form
    '(def x ^{:t {:set [:long]}} #{1 2})
    ;;->
    '(init x #{1 2})
    ;;->
    "let x: rpds::HashTrieSet<i64> = rpds::HashTrieSet::new().insert(1i64).insert(2i64);"))

(deftest user-cast-test
  (inner-form
    '(let [^{:t :int} x (int 1)
           ^{:t :long} y ^{:cast :long} x]
       (println y))
    '(do
       (init x 1)
       (init y x)
       (invoke println y))
    "let x: i32 = 1;
let y: i64 = x as i64;
println!(\"{}\", y);"))

(deftest user-cast-test
  (inner-form
    '(let [^{:t :int} x (int 1)
           ^{:t :long} y ^{:cast :long} x]
       (println y))
    '(do
       (init x 1)
       (init y x)
       (invoke println y))
    "let x: i32 = 1i32;
let y: i64 = x as i64;
println!(\"{}\", y);"))

;; TODO: revisit if necessary, but for now don't bother
(deftest t2
  #_(inner-form
    '(let [^{:t :int} x (int 1)
           ^{:t :int} z (int 3)
           ^{:t :long} y ^{:cast :long} (+ x z)]
       (println y))
    '(do
       (init x 1)
       (init y x)
       (invoke println y))
    "let x: i32 = 1;
let y: i64 = x as i64;
println!(\"{}\", y);"))

;; TODO: for now, you can do this just fine
(deftest t2a
    #_(inner-form
      '(let [^{:t :int} w (int 1)
             ^{:t :int} x (int 3)
             ^{:t :int} y (+ w x)
             ^{:t :long} z ^{:cast :long} y]
         (println z))
      '(do
         (init w 1)
         (init x 3)
         (init y (+ w z))
         (init z y)
         (invoke println z))
      "let w: i32 = 1;
let x: i32 = 3;
let y: i32 = w + x;
let z: i64 = y as i64;
println!(\"{}\", z);"))

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
    "let x: rpds::Vector<rpds::Vector<i64>> = rpds::Vector::new().push_back(rpds::Vector::new().push_back(1i64).clone()).push_back(rpds::Vector::new().push_back(2i64).clone());
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
    "let x: std::collections::HashMap<i64,std::vec::Vec<String>> = {
let mut tmp1: std::collections::HashMap<i64,std::vec::Vec<String>> = std::collections::HashMap::new();
tmp1.insert(1i64, {
let mut tmp2: std::vec::Vec<String> = std::vec::Vec::new();
tmp2.push(String::from(\"hi\"));
tmp2
}.clone());
tmp1.insert(2i64, {
let mut tmp3: std::vec::Vec<String> = std::vec::Vec::new();
tmp3.push(String::from(\"hello\"));
tmp3.push(String::from(\"there\"));
tmp3
}.clone());
tmp1
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
    "let x: rpds::Vector<rpds::HashTrieMap<rpds::HashTrieSet<i64>,rpds::Vector<String>>> = rpds::Vector::new().push_back(rpds::HashTrieMap::new().insert(rpds::HashTrieSet::new().insert(1i64).clone(), rpds::Vector::new().push_back(String::from(\"hi\")).clone()).insert(rpds::HashTrieSet::new().insert(2i64).clone(), rpds::Vector::new().push_back(String::from(\"hello\")).push_back(String::from(\"there\")).clone()).clone());
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
    "let x: rpds::Vector<rpds::HashTrieMap<rpds::HashTrieSet<i64>,rpds::Vector<String>>> = rpds::Vector::new().push_back(rpds::HashTrieMap::new().insert(rpds::HashTrieSet::new().insert(1i64).clone(), rpds::Vector::new().push_back(String::from(\"hi\")).clone()).insert(rpds::HashTrieSet::new().insert(2i64).clone(), rpds::Vector::new().push_back(String::from(\"hello\")).push_back(String::from(\"there\")).clone()).clone());
println!(\"{}\", x);"))

(deftest dataliterals8-1-2-test
  (inner-form
    '(let [^{:t :any} x 1]
       (println x))
    '(do
       (init x 1)
       (invoke println x))
    "let x: kalai::BValue = kalai::BValue::from(1i64);
println!(\"{}\", x);"))

(deftest data-literals8-1-3-test
  (inner-form
    '(let [a 1
           b 2
           c 3
           x ^{:t {:mvector [:any]}}
             [a b c]]
       (println x))
    ;;->
    '(do
       (init a 1)
       (init b 2)
       (init c 3)
       (init x [a b c])
       (invoke println x))
    ;;->
    "let a: i64 = 1i64;
let b: i64 = 2i64;
let c: i64 = 3i64;
let x: std::vec::Vec<kalai::BValue> = {
let mut tmp1: std::vec::Vec<kalai::BValue> = std::vec::Vec::new();
tmp1.push(kalai::BValue::from(a.clone()));
tmp1.push(kalai::BValue::from(b.clone()));
tmp1.push(kalai::BValue::from(c.clone()));
tmp1
};
println!(\"{}\", x);"))

(deftest data-literals8-1-4-test
  (inner-form
    '(let [result (atom ^{:t {:mvector [:any]}} [])
           ^{:t :int} i (atom (int 10))]
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
    "let mut result: std::vec::Vec<kalai::BValue> = std::vec::Vec::new();
let mut i: i32 = 10i32;
while (0i32 < i) {
result.push(kalai::BValue::from(i.clone()));
i = (i - 3i32);
}"))

(deftest data-literals8-1-test
  (inner-form
    '(let [x ^{:t {:mvector [:any]}}
             [1 2 3]]
       (println x))
    ;;->
    '(do
       (init x [1 2 3])
       (invoke println x))
    ;;->
    "let x: kalai::Vector = {
let mut tmp1: kalai::Vector = std::vec::Vec::new();
tmp1.push(kalai::BValue::from(1));
tmp1.push(kalai::BValue::from(2));
tmp1.push(kalai::BValue::from(3));
tmp1
};
println!(\"{}\", x);"))

(deftest data-literals8-1-test
  (inner-form
    '(let [x ^{:t :any} [1 "2" 3]]
       (println x))
    ;;->
    '(do
       (init x [1 "2" 3])
       (invoke println x))
    ;;->
    "let x: kalai::BValue = kalai::BValue::from(rpds::Vector::new().push_back(kalai::BValue::from(1i64)).push_back(kalai::BValue::from(String::from(\"2\"))).push_back(kalai::BValue::from(3i64)));
println!(\"{}\", x);"))

(deftest data-literals8-test
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
    "let x: std::collections::HashMap<String,i64> = {
let mut tmp1: std::collections::HashMap<String,i64> = std::collections::HashMap::new();
tmp1.insert(String::from(\"key\"), (1i64 + 2i64).clone());
tmp1
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
let mut tmp1: std::vec::Vec<i64> = std::vec::Vec::new();
tmp1.push(1i64);
tmp1.push(2i64);
tmp1.push(3i64);
tmp1.push(4i64);
tmp1
} {
println!(\"{}\", x);
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
    "let mut x: i32 = 0i32;
while (x < 5i32) {
println!(\"{}\", x);
x = (x + 1i32);
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
println!(\"{}\", 1i64);
}
else
{
if false
{
println!(\"{}\", 2i64);
}
else
{
if true
{
println!(\"{}\", 3i64);
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
let mut tmp1: std::collections::HashMap<String,i64> = std::collections::HashMap::new();
tmp1.insert(String::from(\":k\"), 1i64);
tmp1
}.get(&String::from(\":k\")).unwrap().clone());"))

(deftest keywords-as-functions2-test
  (inner-form
    '(:k ^{:t {:mset [:string]}} #{:k})
    ;;->
    '(invoke clojure.lang.RT/get #{:k} :k)
    ;;->
    "{
let mut tmp1: std::collections::HashSet<String> = std::collections::HashSet::new();
tmp1.insert(String::from(\":k\"));
tmp1
}.get(&String::from(\":k\")).unwrap().clone();"))

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
    "let k: String = String::from(\"k\");
let m: std::collections::HashMap<String,i64> = {
let mut tmp1: std::collections::HashMap<String,i64> = std::collections::HashMap::new();
tmp1.insert(k.clone(), 1i64);
tmp1
};
let v: i64 = m.get(&k).unwrap().clone();
v;"
    ))

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
1i64
}
else
{
2i64
} + if true
{
if true
{
3i64
}
else
{
4i64
}
}
else
{
5i64
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
println!(\"{}\", 1i64);
2i64
}
else
{
3i64
} + 4i64));"))

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
1i64
}
else
{
if false
{
2i64
}
else
{
3i64
}
} + 4i64));"))

(deftest operator-test
  (inner-form
    '(println
       (not (= 1 (inc 1))))
    ;;->
    '(invoke println
             (operator ! (operator == 1 (operator + 1 1))))
    ;;->
    "println!(\"{}\", !(1i64 == (1i64 + 1i64)));"))


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
    "let a: std::vec::Vec<char> = std::vec::Vec::new();
let b: std::vec::Vec<char> = std::vec::Vec::new();
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
let mut tmp1: std::collections::HashMap<String,i64> = std::collections::HashMap::new();
tmp1.insert(String::from(\":a\"), 1i64);
tmp1
}.insert(String::from(\":b\"), 2i64);"))

(deftest interop3-test
  (inner-form
    '(update ^{:t {:mmap [:string :long]}} {:a 1} :a inc)
    ;;->
    '(invoke update {:a 1} :a inc)
    ;;->
    "{
let mut tmp1: std::collections::HashMap<String,i64> = std::collections::HashMap::new();
tmp1.insert(String::from(\":a\"), 1i64);
tmp1
}.insert(String::from(\":a\").clone(), ({
let mut tmp1: std::collections::HashMap<String,i64> = std::collections::HashMap::new();
tmp1.insert(String::from(\":a\"), 1i64);
tmp1
}.get(&String::from(\":a\")).unwrap().clone() + 1i64));"))

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
    '(let [^{:t {:mvector [:int]}} v [(int 1) (int 2) (int 3)]]
       (println (nth v 1)))
    ;;->
    '(do
       (init v [1 2 3])
       (invoke println
               (invoke clojure.lang.RT/nth v 1)))
    ;;->
    "let v: std::vec::Vec<i32> = {
let mut tmp1: std::vec::Vec<i32> = std::vec::Vec::new();
tmp1.push(1i32);
tmp1.push(2i32);
tmp1.push(3i32);
tmp1
};
println!(\"{}\", v.get(1i64 as usize).unwrap().clone());"))


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
    "let mut result: std::vec::Vec<i32> = std::vec::Vec::new();
let mut i: i32 = 10i32;
while (0i32 < i) {
result.push(i.clone());
i = (i - 3i32);
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
    "let separator_positions: std::vec::Vec<i32> = std::vec::Vec::new();
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
return 1i64;
}
else
{
let x: i64 = 2i64;
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
    "let x: i64 = 1i64;
let y: i64 = x;
println!(\"{}\", y);"))


(deftest propagated-types2-test
  (inner-form
    '(let [^{:t :int} x (int 1)
           y x]
       (println y))
    ;;->
    '(do
       (init x 1)
       (init y x)
       (invoke println y))
    ;;->
    "let x: i32 = 1i32;
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
    "let mut a: i64 = 1i64;
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
    "let mut x: std::vec::Vec<i64> = std::vec::Vec::new();
x = {
let mut tmp1: std::vec::Vec<i64> = std::vec::Vec::new();
tmp1.push(1i64);
tmp1.push(2i64);
tmp1.push(3i64);
tmp1
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
    "let mut x: std::vec::Vec<i64> = std::vec::Vec::new();
x.push(1i64);"))


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
    "let result: std::vec::Vec<char> = std::vec::Vec::new();
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
    "let mut result: std::vec::Vec<i32> = std::vec::Vec::new();
result;"))

(deftest str-test
  (inner-form
    '(str "a" "b")
    '(invoke str "a" "b")
    "format!(\"{}{}\", String::from(\"a\"), String::from(\"b\"));"))

;; TODO: broken, must fix: must support Fn type signature when a value
;; Perhaps as  Box<dyn Fn(_) -> _>   ? -- https://stackoverflow.com/a/65756127
;; More specifically, we want the Box<dyn Fn> to be a BValue (in other words,
;; make a Fn trait obj implement the Value somehow so that we get Box<dyn Value>).
#_(deftest lambda-test
  (inner-form
    '(let [f ^{:t {:function [:int :int]}} (fn [x] x)]
       (f (int 1)))
    '(do
       (init f (lambda [^{:t :int} x]
                       (return x)))
       (invoke f 1))
    "let f: std::ops::Fn(i32) -> i32 = |x| {
return x;
};
f(1i32);"))

(deftest lambda-test2
  (inner-form
    '(map (fn [x] x) ^{:t {:vector [:long]}} [1 2 3])
    '(invoke map
             (lambda [x] (return x))
             [1 2 3])
    "rpds::Vector::new().push_back(1i64).push_back(2i64).push_back(3i64).clone().into_iter().map(|x|{
return x;
});"))
