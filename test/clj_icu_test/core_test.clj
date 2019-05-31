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

(deftest cpp-binding-test
  (testing "cpp - def"
    (let [ast (az/analyze '(def x 3))]
      (is (= "x = 3;" (emit-cpp ast))))
    (let [ast (az/analyze '(def ^Integer x 5))]
      (is (= "int x = 5;" (emit-cpp ast))))))
