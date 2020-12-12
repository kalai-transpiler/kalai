(ns a.demo02
  (:refer-clojure :exclude [format]))

(defn getDigitsMap ^{:t {:mmap [:char :int]}} []
  ^{:t {:mmap [:char :int]}}
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
   \u09EF 9})

(def ^{:t {:mmap [:char :int]}} digitsMap (getDigitsMap))

(defn parse ^Integer [^String s]
  (let [result (atom (int 0))
        ^{:t :int} strLength (count s)]
    (dotimes [^{:t :int} i strLength]
      (let [^{:t :char} digit (nth s i)]
        (if (contains? digitsMap digit)
          (let [^Integer digitVal (get digitsMap digit)]
            (reset! result (+ (* 10 @result) digitVal))))))
    @result))

(defn getNumberSystemsMap
  ^{:t {:mmap [:string {:mvector [:char]}]}} []
  (let [^{:t {:mmap [:string {:mvector [:char]}]}} m
        {"LATIN"   [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9]
         "ARABIC"  [\u0660 \u0661 \u0662 \u0663 \u0664 \u0665 \u0666 \u0667 \u0668 \u0669]
         "BENGALI" [\u09E6 \u09E7 \u09E8 \u09E9 \u09EA \u09EB \u09EC \u09ED \u09EE \u09EF]}]
    m))

(def ^{:t {:mmap [:string {:mvector [:char]}]}}
  numberSystemsMap (getNumberSystemsMap))

(defn getGroupingSeparatorsMap
  ^{:t {:mmap [:string :char]}} []
  ^{:t {:mmap [:string :char]}}
  {"LATIN"   \,
   "ARABIC"  \٬
   "BENGALI" \,})

(def ^{:t {:mmap [:string :char]}}
  groupingSeparatorsMap (getGroupingSeparatorsMap))

(defn getSeparatorPositions ^{:t {:mvector [:int]}}
  [^Integer numLength ^String groupingStrategy]
  (let [^{:t {:mvector [:int]}} result (atom [])]
    (cond
      (= groupingStrategy "NONE")
      @result

      (= groupingStrategy "ON_ALIGNED_3_3")
      (let [^{:t :int} i (atom (- numLength 3))]
        (while (< 0 @i)
          (swap! result conj @i)
          (reset! i (- @i 3)))
        @result)

      (= groupingStrategy "ON_ALIGNED_3_2")
      (let [^{:t :int} i (atom (- numLength 3))]
        (while (< 0 @i)
          (swap! result conj @i)
          (reset! i (- @i 2)))
        @result)

      (= groupingStrategy "MIN_2")
      (if (<= numLength 4)
        @result
        (let [^{:t :int} i (atom (- numLength 3))]
          (while (< 0 @i)
            (swap! result conj @i)
            (reset! i (- @i 3)))
          @result))

      true
      @result)))

(defn format
  ^String [^Integer num, ^String numberSystem, ^String groupingStrategy]
  (let [^{:t :int} i (atom num)
        ^StringBuffer result (StringBuffer.)]
    (while (not (= @i 0))
      (let [^Integer quotient (quot @i 10)
            ^Integer remainder (rem @i 10)
            ^{:t {:mvector [:char]}} numberSystemDigits (get numberSystemsMap numberSystem)
            ^{:t :char} localDigit (get numberSystemDigits remainder)]
        (.insert result 0 localDigit)
        (reset! i quotient)))
    (let [^{:t :char} sep (get groupingSeparatorsMap numberSystem)
          ^{:t :int} numLength (.length result)
          ^{:t {:mvector [:int]}} separatorPositions (getSeparatorPositions numLength groupingStrategy)
          ^{:t :int} numPositions (count separatorPositions)]
      (dotimes [^{:t :int} idx numPositions]
        (let [^int position (nth separatorPositions idx)]
          (.insert result position sep))))
    (.toString result)))

(defn -main ^{:t :void} [& args]
  (println (parse "\u0665\u0660\u0663\u0660\u0661"))
  (println (parse "৫০৩০১"))
  (println (parse "7,654,321"))
  (println (parse "76,54,321"))

  (println (format 7654321 "LATIN" "ON_ALIGNED_3_2"))
  (println (format 7654321 "ARABIC" "ON_ALIGNED_3_3"))
  (println (format 7654321 "BENGALI" "ON_ALIGNED_3_3")))