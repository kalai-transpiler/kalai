(ns a.demo02
  (:refer-clojure :exclude [format]))

(def ^{:kalias {:map [:char :int]}} CI)

(defn getDigitsMap ^{:t CI} []
  (let [^{:t CI} m
        {\0     0
         \1     1
         \2     2
         \3     3
         \4     4
         \5     5
         \6     6
         \7     7
         \8     8
         \9     9
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
    m))

(def ^{:t CI} digitsMap (getDigitsMap))

(defn parse ^Integer [^String s]
  (let [result (atom 0)
        strLength (count s)]
    (dotimes [^{:t Integer} i strLength]
      (let [^Character digit (nth s i)]
        (if (contains? digitsMap digit)
          (let [^Integer digitVal (get digitsMap digit)]
            (reset! result (+ (* 10 @result) digitVal))))))
    @result))

(def ^{:kalias {:map [:string {:list [:char]}]}} SLC)

(defn getNumberSystemsMap
  ^{:t SLC} []
  (let [^{:t SLC} m
        {"LATIN"   [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9]
         "ARABIC"  [\u0660 \u0661 \u0662 \u0663 \u0664 \u0665 \u0666 \u0667 \u0668 \u0669]
         "BENGALI" [\u09E6 \u09E7 \u09E8 \u09E9 \u09EA \u09EB \u09EC \u09ED \u09EE \u09EF]}]
    m))

(def ^{:t SLC}
  numberSystemsMap (getNumberSystemsMap))

(def ^{:kalias {:map [:string :char]}} SC)

(defn getGroupingSeparatorsMap
  ^{:t SC} []
  (let [^{:t SC} m
        {"LATIN"   \,
         "ARABIC"  \٬
         "BENGALI" \,}]
    m))

(def ^{:t SC}
  groupingSeparatorsMap (getGroupingSeparatorsMap))

(def ^{:kalias {:list [:int]}} LI)

(defn getSeparatorPositions ^{:t LI}
  [^Integer numLength ^String groupingStrategy]
  (let [^{:t {:list [:int]}} result (atom [])]
    (cond
      (= groupingStrategy "NONE")
      @result

      (= groupingStrategy "ON_ALIGNED_3_3")
      (let [i (atom (- numLength 3))]
        (while (< 0 @i)
          (swap! result conj @i)
          (reset! i (- @i 3)))
        @result)

      (= groupingStrategy "ON_ALIGNED_3_2")
      (let [i (atom (- numLength 3))]
        (while (< 0 @i)
          (swap! result conj @i)
          (reset! i (- @i 2)))
        @result)

      (= groupingStrategy "MIN_2")
      (if (<= numLength 4)
        @result
        (let [i (atom (- numLength 3))]
          (while (< 0 @i)
            (swap! result conj @i)
            (reset! i (- @i 3)))
          @result))

      :else
      @result)))

(defn format
  ^String [^Integer num, ^String numberSystem, ^String groupingStrategy]
  (let [i (atom num)
        ^StringBuffer result (StringBuffer.)]
    (while (not (= @i 0))
      (let [^Integer quotient (quot @i 10)
            ^Integer remainder (rem @i 10)
            ^{:t {List [Character]}} numberSystemDigits (get numberSystemsMap numberSystem)
            ^Character localDigit (nth numberSystemDigits remainder)]
        (.insert result 0 localDigit)
        (reset! i quotient)))
    (let [^Character sep (get groupingSeparatorsMap numberSystem)
          numLength (.length result)
          ^{:t LI} separatorPositions (getSeparatorPositions numLength groupingStrategy)
          numPositions (count separatorPositions)]
      (dotimes [idx numPositions]
        (let [^Integer position (nth separatorPositions idx)]
          (reset! result (.insert result position sep)))))
    (.toString result)))
