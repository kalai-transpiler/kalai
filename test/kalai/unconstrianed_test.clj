(ns kalai.unconstrianed-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.test-helpers :refer [top-level-form inner-form]]))

(deftest t1
  (top-level-form
    '(def ^{:t "int"} x 3)
    ;;->
    '(init false "int" x 3)
    ;;->
    "int x = 3;"))

(deftest t15
  (top-level-form
    '(def ^Integer x)
    ;;->
    '(init false Integer x)
    ;;->
    "Integer x;"))

(deftest t16
  (top-level-form
    '(defn f ^Integer [^Integer x]
       (inc x))
    ;;->
    '(function f Integer nil [x]
               (return (operator + x 1)))
    ;;->
    "public static Integer f(Integer x) {
return (x+1);
}"))

(deftest t17
  (top-level-form
    '(defn f []
       (let [^int x (atom 0)]
         (swap! x inc)))
    ;;->
    '(function f nil nil []
       (do
         (init true int x 0)
         (assign x (invoke inc x))
         (return x)))
    ;;->
    "public static  f() {
int x = 0;
x=inc(x);
return x;
}"))

(deftest t165
  (top-level-form
    '(defn f
       (^int [^int x]
        (inc x))
       (^int [^int x ^int y]
        (+ x y)))
    ;;->
    '(function f int nil [x]
               (return (operator + x 1)))
    ;;->
    "public static int f(int x) {
return (x+1);
}
public static int f(int x, int y) {
return (x+y);
}"))

;; Tim proposes: let's not support void!!!
;; It's not necessary for writing programs... returning null is equivalent.
;; If you want to write a side effect function, it should return type Object and return null.
;; That means Kalai can continue to follow the do it the Clojure way mantra.
#_
(deftest t17
  (top-level-form
    '(defn f ^void [^int x]
       (println x))
    ;;->
    '(function f void nil [x]
               (println x))
    ;;->
    "public static void f(int x) {
(x+1);
}"))

(deftest t18
  (inner-form
    '(let [^int x (atom 1)
           ^int y (atom 2)
           ^int z 1]
       (reset! x 3)
       (+ @x (deref y)))
    ;;->
    '(do
       (init true int x 1)
       (init true int y 2)
       (init false int z 1)
       (do
         (assign x 3)
         (operator + x y)))
    ;;->
    "{
int x = 1;
int y = 2;
int z = 1;
{
x=3;
(x+y);
}
}"))

(deftest t19
  (inner-form
    '(with-local-vars [^int x 1
                       ^int y 2]
       (+ (var-get x) (var-get y)))
    ;;->
    '(do
       (init true int x 1)
       (init true int y 2)
       (operator + x y))
    ;;->
    "{
int x = 1;
int y = 2;
(x+y);
}"))

#_
(deftest t111
  (top-level-form
    '(do (def ^{:kalias '[kmap [klong kstring]]} T)
         (def ^{:t T} x))
    ;;->
    '()
    ;;->
    ""))

;; this test covers type erasure, but we have disabled that
;; as the bottom up traversal does too much (slow)
#_
(deftest t112
  (top-level-form
    '(do (def ^{:t ['kmap ['klong 'kstring]]} x))
    ;;->
    '()
    ;;->
    ""))

(deftest t2
  (inner-form
    '(do (def ^{:t "Boolean"} x true)
         (def ^{:t "Long"} y 5))
    ;;->
    '(do
       (init false "Boolean" x true)
       (init false "Long" y 5))
    ;;->

    ;; TODO: condense could have stripped
    "{
Boolean x = true;
Long y = 5;
}"))

(deftest t3-0
  (inner-form
    '(let [^int x 1]
       x)
    ;;->
    '(do
       (init false int x 1)
       x)
    ;;->
    "{
int x = 1;
x;
}"))

(deftest t3-1
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

(deftest t3-1-1
  (inner-form
    '(let [^int x (atom 0)]
       (reset! x (+ @x 2)))
    ;;->
    '(do
       (init true int x 0)
       (assign x (operator + x 2)))
    ;;->
    "{
int x = 0;
x=(x+2);
}"
    ))

#_(let [^:mut x (mut [1 2 3])]
    (mconj x 4)
    x)
#_(let [x (Vector. 1 2 3)])

#_(mlet [x [1 2 3]]
        (mconj x 4))


(deftest t3-2
  #_(inner-form
      [1 2]
      ;;->
      ;;"Vector<Integer> v = new Vector<>(); v.add(1); v.add(2);" ;;possible
      "new Vector<Integer>().add(1).add(2)" ;; requires non-core
      ))

(deftest t4
  (inner-form
    '(doseq [^int x [1 2 3 4]]
       (println x))
    ;;->
    '(foreach int x [1 2 3 4]
              (invoke println x))
    ;;->
    "for (int x : [1 2 3 4]) {
System.out.println(x);
}"))

(deftest t5
  (inner-form
    '(dotimes [x 5]
       (println x))
    ;;->
    '(init true int x 0)
    ;;->
    "int x = 0;
while ((x<5)) {
System.out.println(x);
x=(x+1);
}"))

(deftest t3
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

(deftest test6
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

(deftest test6*
  #_(inner-form
      '(:k {:k 1})
      ;;->
      "zzz"
      ))

(deftest test7
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

(deftest test75
  #_(inner-form
      '(def ^{:t [String String]} x {:a "asdf"})
      ;;->
      "x = new HashMap<String,String>();
  x.add(\":a\", \"asdf\""))

(deftest test8
  #_(inner-form
      '(assoc {:a 1} :b 2)
      ;;->
      ""))
