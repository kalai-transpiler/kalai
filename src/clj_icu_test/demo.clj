(ns clj-icu-test.demo
  (:require [clj-icu-test.core :refer :all]))

(defclass "NumFmt"
  (defn parse ^String [^Integer num]
    (let [^Integer i (atom num)
          ^String result (atom "")]
      (while (not (= @i 0))
        (let [^Integer quotient (quot @i 10)
              ^Integer remainder (rem @i 10)]
          (reset! result (str remainder @result))
          (reset! i quotient)))
      (return @result))))
