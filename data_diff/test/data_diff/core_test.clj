(ns data-diff.core-test
  (:require [data-diff.core :as d]
            [clojure.data :as data]
            [clojure.test :refer :all]))

(deftest diff-test
  (let [first-five {:a 1, :b 2, :c 3, :d 4, :e 5}
        vowels {:a 1, :e 5, :i 9, :o 15, :u 21}]
    (is (= (d/diff-associative first-five vowels [:a])
           [nil nil {:a 1}]))
    (is (= (d/diff first-five vowels)
           [{:b 2, :c 3, :d 4} {:i 9, :o 15, :u 21} {:a 1, :e 5}]))
    (is (= (d/diff first-five vowels)
           (data/diff first-five vowels)))
    (is (= (d/diff (set (vals first-five))
                   (set (vals vowels)))
           (data/diff (set (vals first-five))
                      (set (vals vowels)))))
    (is (= (d/diff (vec (vals first-five))
                   (vec (vals vowels)))
           (data/diff (vec (vals first-five))
                      (vec (vals vowels)))))
    (is (= (d/diff {:x first-five, :y vowels}
                   {:x vowels :y first-five})
           (data/diff {:x first-five, :y vowels}
                      {:x vowels :y first-five})))))
