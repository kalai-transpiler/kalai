(ns clj-icu-test.core-test
  (:require [clojure.test :refer :all]
            [clojure.tools.analyzer.jvm :as az]
            [clj-icu-test.core :refer :all]))

(deftest a-test
  (testing "java - def"
    (let [ast (az/analyze '(def x 3))] 
      (is (= "x = 3;" (emit-java ast))))
    (let [ast (az/analyze '(def ^Integer x 3))]
      (is (= "Integer x = 3;" (emit-java ast))))))
