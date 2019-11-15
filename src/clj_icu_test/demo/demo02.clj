(ns clj-icu-test.demo.demo02
  (:require [clj-icu-test.common :refer :all])
  (:import [java.util List Map]))

(defclass "NumFmt"

  (def ^{:mtype [Map [String [List [Character]]]]}
    numberSystemsMap {"LATIN" [\0 \1 \9]})

  (defn getNumberSystemsMap ^{:mtype [Map [String [List [Character]]]]} []
    (let [^{:mtype [Map [String [List [Character]]]]} m {"LATIN" [\0 \1 \9]}]
      (return m)))
  
  (defn format ^String [^Integer num]
    (let [^Integer i (atom num)
          ^StringBuffer result (atom (new-strbuf))]
      (while (not (= @i 0))
        (let [^Integer quotient (quot @i 10)
              ^Integer remainder (rem @i 10)]
          (reset! result (prepend-strbuf @result remainder))
          (reset! i quotient)))
      (return (tostring-strbuf @result)))))
