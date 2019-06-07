(ns clj-icu-test.core-test
  (:require [clojure.test :refer :all]
            [clojure.tools.analyzer.jvm :as az]
            [clj-icu-test.core :refer :all]))

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

(deftest cpp-binding-test
  (testing "cpp - def"
    (let [ast (az/analyze '(def x 3))]
      (is (= "x = 3;" (emit-cpp ast))))
    (let [ast (az/analyze '(def ^Integer x 5))]
      (is (= "int x = 5;" (emit-cpp ast))))))
