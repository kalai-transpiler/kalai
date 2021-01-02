(ns kalai.pass.rust-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.pass.test-helpers :refer [ns-form-rust top-level-form-rust inner-form-rust]]))

;; # Creating Variables

;; TODO: this should be a static (top level "def")
(deftest init1-test
  (top-level-form-rust
    '(def ^{:t :int} x 3)
    ;;->
    '(init x 3)
    ;;->
    "let x: i32 = 3;"))

(deftest init2-test
  (top-level-form-rust
    '(def ^Integer x)
    ;;->
    '(init x)
    ;;->
    "let x: i32;"))

(deftest init3-test
  (inner-form-rust
    '(let [^{:t :int} x 1]
       x)
    ;;->
    '(do
       (init x 1)
       x)
    ;;->
    "let x: i32 = 1;"))

;; TODO: do we even need to support this for Rust?
#_(deftest init4-test
  (inner-form-rust
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
  #_(top-level-form-rust
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
    (top-level-form-rust
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
      (top-level-form-rust
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
  (ns-form-rust
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
  (top-level-form-rust
    '(defn f ^{:t :void} [^long x]
       (println x x))
    ;;->
    '(function f [x]
               (invoke println x x))
    ;;->
    "pub fn f(x: i64) -> () {
println!(\"{} {}\", x, x);
}"))