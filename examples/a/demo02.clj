(ns examples.a.demo02
  (:refer-clojure :exclude [format])
  (:require [kalai.common :refer :all])
  (:import (java.util List Map)))

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

  (defn parse ^Integer [^String s]
    (let [^Integer result (atom 0)
          ^Integer strLength (strlen s)]
      (dotimes [^{:mtype Integer} i strLength]
        (let [^Character digit (str-char-at s i)]
          (if (contains? digitsMap digit)
            (let [^Integer digitVal (get digitsMap digit)]
              (reset! result (+ (* 10 @result) digitVal))))))
      (return @result)))

  (defn getNumberSystemsMap ^{:mtype [Map [String [List [Character]]]]}
    []
    (let [^{:mtype [Map [String [List [Character]]]]} m {"LATIN" [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9]
                                                         "ARABIC" [\u0660 \u0661 \u0662 \u0663 \u0664 \u0665 \u0666 \u0667 \u0668 \u0669]
                                                         "BENGALI" [\u09E6 \u09E7 \u09E8 \u09E9 \u09EA \u09EB \u09EC \u09ED \u09EE \u09EF]}]
      (return m)))

  (def ^{:mtype [Map [String [List [Character]]]]}
    numberSystemsMap (getNumberSystemsMap))

  (defn getGroupingSeparatorsMap ^{:mtype [Map [String Character]]}
    []
    (let [^{:mtype [Map [String Character]]} m {"LATIN" \,
                                                "ARABIC" \٬
                                                "BENGALI" \,}]
      (return m)))

  (def ^{:mtype [Map [String Character]]}
    groupingSeparatorsMap (getGroupingSeparatorsMap))

  (defn getSeparatorPositions ^{:mtype [List [Integer]]}
    [^Integer numLength ^String groupingStrategy]
    (let [^{:mtype [List [Integer]]} result (atom [])]

      (cond
        
        (str-eq groupingStrategy "NONE")
        (return @result)

        (str-eq groupingStrategy "ON_ALIGNED_3_3")
        (let [^Integer i (atom (- numLength 3))]
          (while (< 0 @i)              
            (seq-append result @i)
            (reset! i (- @i 3)))
          (return @result))
        
        (str-eq groupingStrategy "ON_ALIGNED_3_2")
        (let [^Integer i (atom (- numLength 3))]
          (while (< 0 @i)
            (seq-append result @i)
            (reset! i (- @i 2)))
          (return @result))
        
        (str-eq groupingStrategy "MIN_2")
        (if (<= numLength 4)
          (return @result)
          (let [^Integer i (atom (- numLength 3))]
            (while (< 0 @i)
              (seq-append result @i)
              (reset! i (- @i 3)))
            (return @result)))

        :else
        (return @result))))
  
  (defn format ^String [^Integer num, ^String numberSystem, ^String groupingStrategy]
    (let [^Integer i (atom num)
          ^StringBuffer result (atom (new-strbuf))]
      (while (not (= @i 0))
        (let [^Integer quotient (quot @i 10)
              ^Integer remainder (rem @i 10)
              ^{:mtype [List [Character]]} numberSystemDigits (get numberSystemsMap numberSystem)
              ^Character localDigit (nth numberSystemDigits remainder)]
          (prepend-strbuf @result localDigit)
          (reset! i quotient)))
      (let [^Character sep (get groupingSeparatorsMap numberSystem)
            ^Integer numLength (length-strbuf @result)
            ^{:mtype [List [Integer]]} separatorPositions (getSeparatorPositions numLength groupingStrategy)
            ^Integer numPositions (seq-length separatorPositions)]
        (dotimes [^{:mtype Integer} idx numPositions]
          (let [^Integer position (nth separatorPositions idx)]
            (reset! result (insert-strbuf-char @result position sep)))))
      (return (tostring-strbuf @result)))))
