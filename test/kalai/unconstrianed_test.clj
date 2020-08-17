(ns kalai.unconstrianed-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.test-helpers :refer [top-level-form inner-form]]))

(deftest t1
  (top-level-form
    '(def ^{:t "int"} x 3)
    ;;->
    "int x = 3;"))

(deftest t15
  (top-level-form
    '(def ^Integer x)
    ;;->
    "Integer x;"))

(deftest t16
  (top-level-form
    '(defn f ^Integer [^Integer x]
       (inc x))
    ;;->
    "public static Integer f(Integer x) {
return (x+1);
}"))

(deftest t165
  (top-level-form
    '(defn f
       (^int [^int x]
        (inc x))
       (^int [^int x ^int y]
        (+ x y)))
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
(deftest t17
  (top-level-form
    '(defn f ^void [^int x]
       (inc x))
    ;;->
    "public static void f(int x) {
(x+1);
}"))

(deftest t2
  (inner-form
    '(do (def ^{:t "Boolean"} x true)
         (def ^{:t "Long"} y 5))
    ;;->
    "Boolean x = true;
Long y = 5;"))

(deftest t3
  (inner-form
    '(let [^int x 1])
    ;;->
    "int x = 1;"))

(deftest t4-5
  (inner-form
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

(deftest t4
  (inner-form
    '(doseq [x [1 2 3 4]]
       (println x))
    ;;->
    ""))

(deftest t5
  (inner-form
    '(dotimes [x 5]
       (println x))
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
    "while (true) {
System.out.println(\"hi\");
}"))

(deftest test6
  (inner-form
    '(cond true 1
           false 2
           :else 3)
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
if (:else)
{
3;
}
}
}"))

(deftest test7
  (inner-form
    '(case 1
       1 :a
       2 :b)
    ;;->
    ""))

(deftest test75
  (inner-form
    '(def ^{:t ['String 'String]} x {:a "asdf"})
    ;;->
    "x = new HashMap<String,String>();
x.add(\":a\", \"asdf\""))

(deftest test8
  (inner-form
    '(assoc {:a 1} :b 2)
    ;;->
    ""))
