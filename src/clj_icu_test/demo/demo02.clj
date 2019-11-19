(ns clj-icu-test.demo.demo02
  (:require [clj-icu-test.common :refer :all])
  (:import [java.util List Map]))

(defclass "NumFmt"

  (defn getDigitsMap ^{:mtype [Map [Character Integer]]}
    []
    (let [^{:mtype [Map [Character Integer]]} m {\0 0
                                                 \1 1
                                                 \2 2
                                                 \3 3
                                                 \4 4
                                                 \5 5
                                                 \6 6
                                                 \7 7
                                                 \8 8
                                                 \9 9
                                                 \u0660 0
                                                 \u0661 1
                                                 \u0662 2
                                                 \u0663 3
                                                 \u0664 4
                                                 \u0665 5
                                                 \u0666 6
                                                 \u0667 7
                                                 \u0668 8
                                                 \u0669 9
                                                 \u09E6 0
                                                 \u09E7 1
                                                 \u09E8 2
                                                 \u09E9 3
                                                 \u09EA 4
                                                 \u09EB 5
                                                 \u09EC 6
                                                 \u09ED 7
                                                 \u09EE 8
                                                 \u09EF 9}]
      (return m)))

  (def ^{:mtype [Map [Character Integer]]} digitsMap (getDigitsMap))

  (defn parse ^Integer [^String str]
    (let [^Integer result (atom 0)
          ^Integer strLength (strlen str)]
      (dotimes [^{:mtype Integer} i 10]
        (let [^Character digit (nth str i)
              ^Integer digitVal (get digitsMap digit)]
          (reset! result (+ (* 10 @result) digitVal))))
      (return result)))

  (defn getNumberSystemsMap ^{:mtype [Map [String [List [Character]]]]}
    []
    (let [^{:mtype [Map [String [List [Character]]]]} m {"LATIN" [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9]
                                                         "ARABIC" [\u0660 \u0661 \u0662 \u0663 \u0664 \u0665 \u0666 \u0667 \u0668 \u0669]
                                                         "BENGALI" [\u09E6 \u09E7 \u09E8 \u09E9 \u09EA \u09EB \u09EC \u09ED \u09EE \u09EF]}]
      (return m)))

  (def ^{:mtype [Map [String [List [Character]]]]}
    numberSystemsMap (getNumberSystemsMap))
  
  (defn format ^String [^Integer num, ^String numberSystem]
    (let [^Integer i (atom num)
          ^StringBuffer result (atom (new-strbuf))]
      (while (not (= @i 0))
        (let [^Integer quotient (quot @i 10)
              ^Integer remainder (rem @i 10)
              ^{:mtype [List [Character]]} numberSystemDigits (get numberSystemsMap numberSystem)
              ^Character localDigit (nth numberSystemDigits remainder)]
          (reset! result (prepend-strbuf @result localDigit))
          (reset! i quotient)))
      (return (tostring-strbuf @result)))))
