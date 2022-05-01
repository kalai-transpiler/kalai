(ns data-diff.core-test
  (:require [data-diff.core :as d]
            [clojure.test :refer :all]))

(deftest diff-test
  (is (= (d/diff-associative {:a 1 :b 2 :c 3 :d 4 :e 5}
                             {:a 1 :e 5 :i 9 :o 14 :u 21}
                             [:a])
         [nil nil {:a 1}])))