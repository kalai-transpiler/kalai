(ns clj-icu-test.core-test
  (:require [clojure.test :refer :all]
            [clojure.tools.analyzer.jvm :as az]
            [clj-icu-test.core :refer :all]))
;;
;; C++
;;

(deftest cpp-binding-test
  (testing "cpp - def"
    (let [ast (az/analyze '(def x 3))]
      (is (= "x = 3;" (emit-cpp ast))))
    (let [ast (az/analyze '(def ^Integer x 5))]
      (is (= "int x = 5;" (emit-cpp ast))))))

;;
;; Java
;;

(deftest java-binding-test
  (testing "java - def"
    (let [ast (az/analyze '(def x 3))] 
      (is (= "x = 3;" (emit-java ast))))
    (let [ast (az/analyze '(def ^Integer x 5))]
      (is (= "Integer x = 5;" (emit-java ast))))))

(deftest java-multiple-expressions
  (testing "java - do"
    (let [ast (az/analyze '(do (def x 3) (def y 5)))]
      (is (= (emit-java ast) ["x = 3;"
                              "y = 5;"])))
    (let [ast (az/analyze '(do (def ^Boolean x true) (def ^Long y 5)))]
      (is (= (emit-java ast) ["Boolean x = true;"
                              "Long y = 5;"])))))

(deftest java-binding-test-atoms
  (testing "java - atom"
    (let [ast (az/analyze '(def x (atom 11)))]
      (is (= (emit-java ast) "x = 11;"))))
  (testing "java - reset!"
    (let [ast (az/analyze '(do (def x (atom 11)) (reset! x 13)))]
      (is (= (emit-java ast) ["x = 11;"
                              "x = 13;"])))
    (let [ast (az/analyze '(do (def ^Long x (atom 11)) (reset! x 13)))]
      (is (= (emit-java ast) ["Long x = 11;"
                              "x = 13;"])))))

(deftest java-static-call-test
  (testing "java - arithmetic fn - +"
    (let [ast (az/analyze '(+ 11 17))]
      (is (= (emit-java ast) "11 + 17"))))
  (testing "java - arithmetic fn - /"
    (let [ast (az/analyze '(/ 34 17))]
      (is (= (emit-java ast) "34 / 17"))))
  (testing "java - arithmetic fn - multiple operands"
    (let [ast (az/analyze '(+ 11 17 19 23))]
      (is (= (emit-java ast) "11 + 17 + 19 + 23")))))

(deftest java-binding-test-let
  (testing "java - let - 1 expression"
    (let [ast (az/analyze '(let [x 1] (+ x 3)))]
      (is (= (emit-java ast)
"{
  x = 1;
  x + 3;
}"))))
  (testing "java - let - 2 expressions"
    (let [ast (az/analyze '(let [x 1] (+ x 3) (+ x 5)))]
      (is (= (emit-java ast)
"{
  x = 1;
  x + 3;
  x + 5;
}"))))
  (testing "java - let - 2 bindings"
    (let [ast (az/analyze '(let [x 1 y 2] (* x y)))]
      (is (= (emit-java ast)
"{
  x = 1;
  y = 2;
  x * y;
}"))))
  (testing "java - let - 2 bindings - expression in binding"
    (let [ast (az/analyze '(let [x 5 y (* x x)] (+ x y)))]
      (is (= (emit-java ast)
"{
  x = 5;
  y = x * x;
  x + y;
}")))))
