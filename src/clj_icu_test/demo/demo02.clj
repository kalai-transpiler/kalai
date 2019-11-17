(ns clj-icu-test.demo.demo02
  (:require [clj-icu-test.common :refer :all])
  (:import [java.util List Map]))

(defclass "NumFmt" 

  (defn getNumberSystemsMap ^{:mtype [Map [String [List [Character]]]]} []
    (let [^{:mtype [Map [String [List [Character]]]]} m {"LATIN" [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9]
                                                         "ARABIC" [\u0660 \u0661 \u0662 \u0663 \u0664 \u0665 \u0666 \u0667 \u0668 \u0669]
                                                         "BENGALI" [\u09E6 \u09E7 \u09E8 \u09E9 \u09EA \u09EB \u09EC \u09ED \u09EE \u09EF]}]
      (return m)))

  (def ^{:mtype [Map [String [List [Character]]]]}
    numberSystemsMap (getNumberSystemsMap))
  
  (defn format ^String [^Integer num]
    (let [^Integer i (atom num)
          ^StringBuffer result (atom (new-strbuf))]
      (while (not (= @i 0))
        (let [^Integer quotient (quot @i 10)
              ^Integer remainder (rem @i 10)]
          (reset! result (prepend-strbuf @result remainder))
          (reset! i quotient)))
      (return (tostring-strbuf @result)))))
